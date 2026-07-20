package com.tnf.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tnf.auth_service.config.JwtProperties;
import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.service.impl.JwtServiceImpl;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;

    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-that-is-long-enough-0123456789abcd");
        properties.setIssuer("auth-service");
        properties.setAccessTokenExpirationMs(900_000L);
        properties.setRefreshTokenExpirationMs(604_800_000L);
        jwtService = new JwtServiceImpl(properties);

        user = User.builder()
                .id("user-1")
                .username("alice")
                .email("alice@example.com")
                .password("hash")
                .roles(Set.of("ROLE_USER", "ROLE_ADMIN"))
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("generated token is valid and round-trips subject and roles")
    void generatesValidToken() {
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractRoles(token)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("a tampered token is rejected")
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("garbage input is not a valid token")
    void rejectsGarbage() {
        assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("token signed with a different secret is rejected")
    void rejectsForeignSignature() {
        JwtProperties other = new JwtProperties();
        other.setSecret("a-completely-different-secret-key-0123456789-abcdefgh");
        other.setIssuer("auth-service");
        String foreign = new JwtServiceImpl(other).generateAccessToken(user);

        assertThat(jwtService.isTokenValid(foreign)).isFalse();
    }
}
