package com.tnf.auth_service.service.impl;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.exception.RoleNotFoundException;
import com.tnf.auth_service.exception.UserAlreadyExistsException;
import com.tnf.auth_service.exception.UserNotFoundException;
import com.tnf.auth_service.repository.RoleRepository;
import com.tnf.auth_service.repository.UserRepository;
import com.tnf.auth_service.service.UserService;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

/**
 * Default {@link UserService}. Enforces unique username/email, hashes passwords with the configured
 * {@link PasswordEncoder} and validates requested roles against the seeded role set.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        Set<String> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Registered new user '{}' with roles {}", saved.getUsername(), saved.getRoles());
        return saved;
    }

    @Override
    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("No user found with username: " + username));
    }

    @Override
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles())
                .enabled(user.isEnabled())
                .build();
    }

    /** Defaults to ROLE_USER when none requested; otherwise verifies each requested role exists. */
    private Set<String> resolveRoles(Set<String> requested) {
        if (requested == null || requested.isEmpty()) {
            return new HashSet<>(Set.of(DEFAULT_ROLE));
        }
        Set<String> resolved = new HashSet<>();
        for (String role : requested) {
            if (!roleRepository.existsByName(role)) {
                throw new RoleNotFoundException("Unknown role: " + role);
            }
            resolved.add(role);
        }
        return resolved;
    }
}
