package com.tnf.auth_service.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.tnf.auth_service.entity.RefreshToken;
import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.exception.InvalidCredentialsException;
import com.tnf.auth_service.exception.UserNotFoundException;
import com.tnf.auth_service.repository.UserRepository;
import com.tnf.auth_service.security.CustomUserDetails;
import com.tnf.auth_service.service.AuthService;
import com.tnf.auth_service.service.JwtService;
import com.tnf.auth_service.service.RefreshTokenService;
import com.tnf.auth_service.service.UserService;
import com.tnf.common_dto.dto.auth.JwtResponse;
import com.tnf.common_dto.dto.auth.LoginRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenResponse;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

/**
 * Default {@link AuthService}. Coordinates {@link UserService}, {@link JwtService} and
 * {@link RefreshTokenService} to implement the full authentication flow.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String TOKEN_TYPE = "Bearer";

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserService userService,
            UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public JwtResponse register(RegisterRequest request) {
        User user = userService.register(request);
        log.info("User '{}' registered; issuing initial token pair", user.getUsername());
        return issueTokenPair(user);
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            User user = ((CustomUserDetails) authentication.getPrincipal()).getUser();
            log.info("User '{}' authenticated successfully", user.getUsername());
            return issueTokenPair(user);
        } catch (BadCredentialsException ex) {
            log.warn("Failed login attempt for username '{}'", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Override
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenService.validate(request.getRefreshToken());
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User for refresh token no longer exists"));

        // Rotation: the presented token is revoked and a brand-new one is issued.
        refreshTokenService.revoke(stored.getToken());
        RefreshToken rotated = refreshTokenService.create(user.getId());
        log.info("Rotated refresh token for user '{}'", user.getUsername());

        return RefreshTokenResponse.builder()
                .accessToken(jwtService.generateAccessToken(user))
                .refreshToken(rotated.getToken())
                .tokenType(TOKEN_TYPE)
                .build();
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        log.info("Refresh token revoked via logout");
    }

    @Override
    public UserResponse getCurrentUser(String username) {
        return userService.toResponse(userService.getByUsername(username));
    }

    private JwtResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.create(user.getId());
        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType(TOKEN_TYPE)
                .username(user.getUsername())
                .roles(user.getRoles())
                .build();
    }
}
