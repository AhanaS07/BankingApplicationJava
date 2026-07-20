package com.tnf.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.tnf.auth_service.entity.RefreshToken;
import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.exception.InvalidCredentialsException;
import com.tnf.auth_service.repository.UserRepository;
import com.tnf.auth_service.security.CustomUserDetails;
import com.tnf.auth_service.service.impl.AuthServiceImpl;
import com.tnf.common_dto.dto.auth.JwtResponse;
import com.tnf.common_dto.dto.auth.LoginRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenResponse;
import com.tnf.common_dto.dto.auth.RegisterRequest;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(authenticationManager, userService, userRepository, jwtService,
                refreshTokenService);
        user = User.builder()
                .id("user-1").username("alice").email("alice@example.com")
                .password("hash").roles(Set.of("ROLE_USER")).enabled(true).build();
    }

    private RefreshToken refreshToken(String value) {
        return RefreshToken.builder().token(value).userId("user-1")
                .expiryDate(Instant.now().plusSeconds(60)).revoked(false).build();
    }

    @Test
    void registerIssuesTokenPair() {
        when(userService.register(any(RegisterRequest.class))).thenReturn(user);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.create("user-1")).thenReturn(refreshToken("refresh"));

        JwtResponse response = authService.register(RegisterRequest.builder()
                .username("alice").email("alice@example.com").password("Str0ng@Pass").build());

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo("alice");
        assertThat(response.getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void loginReturnsTokensOnValidCredentials() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                CustomUserDetails.from(user), null, java.util.List.of());
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(refreshTokenService.create("user-1")).thenReturn(refreshToken("refresh"));

        JwtResponse response = authService.login(new LoginRequest("alice", "Str0ng@Pass"));

        assertThat(response.getAccessToken()).isEqualTo("access");
        assertThat(response.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void loginTranslatesBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshRotatesToken() {
        when(refreshTokenService.validate("old")).thenReturn(refreshToken("old"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(refreshTokenService.create("user-1")).thenReturn(refreshToken("new"));
        when(jwtService.generateAccessToken(user)).thenReturn("access2");

        RefreshTokenResponse response = authService.refresh(new RefreshTokenRequest("old"));

        assertThat(response.getAccessToken()).isEqualTo("access2");
        assertThat(response.getRefreshToken()).isEqualTo("new");
        verify(refreshTokenService).revoke("old");
    }

    @Test
    void logoutRevokesToken() {
        authService.logout(new RefreshTokenRequest("some-token"));

        verify(refreshTokenService).revoke("some-token");
    }
}
