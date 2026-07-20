package com.tnf.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tnf.auth_service.config.JwtProperties;
import com.tnf.auth_service.entity.RefreshToken;
import com.tnf.auth_service.exception.RefreshTokenExpiredException;
import com.tnf.auth_service.exception.RefreshTokenNotFoundException;
import com.tnf.auth_service.repository.RefreshTokenRepository;
import com.tnf.auth_service.service.impl.RefreshTokenServiceImpl;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository repository;

    private RefreshTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-that-is-long-enough-0123456789abcd");
        properties.setRefreshTokenExpirationMs(604_800_000L);
        service = new RefreshTokenServiceImpl(repository, properties);
    }

    @Test
    void createPersistsTokenForUser() {
        when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken token = service.create("user-1");

        assertThat(token.getUserId()).isEqualTo("user-1");
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getExpiryDate()).isAfter(Instant.now());
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void validateReturnsUsableToken() {
        RefreshToken stored = RefreshToken.builder()
                .token("abc").userId("user-1")
                .expiryDate(Instant.now().plusSeconds(60)).revoked(false).build();
        when(repository.findByToken("abc")).thenReturn(Optional.of(stored));

        assertThat(service.validate("abc")).isSameAs(stored);
    }

    @Test
    void validateRejectsUnknownToken() {
        when(repository.findByToken("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validate("nope"))
                .isInstanceOf(RefreshTokenNotFoundException.class);
    }

    @Test
    void validateRejectsRevokedToken() {
        RefreshToken stored = RefreshToken.builder()
                .token("abc").userId("user-1")
                .expiryDate(Instant.now().plusSeconds(60)).revoked(true).build();
        when(repository.findByToken("abc")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.validate("abc"))
                .isInstanceOf(RefreshTokenExpiredException.class);
    }

    @Test
    void validateRejectsExpiredTokenAndDeletesIt() {
        RefreshToken stored = RefreshToken.builder()
                .token("abc").userId("user-1")
                .expiryDate(Instant.now().minusSeconds(60)).revoked(false).build();
        when(repository.findByToken("abc")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.validate("abc"))
                .isInstanceOf(RefreshTokenExpiredException.class);
        verify(repository).delete(stored);
    }

    @Test
    void revokeMarksTokenRevoked() {
        RefreshToken stored = RefreshToken.builder()
                .token("abc").userId("user-1")
                .expiryDate(Instant.now().plusSeconds(60)).revoked(false).build();
        when(repository.findByToken("abc")).thenReturn(Optional.of(stored));

        service.revoke("abc");

        assertThat(stored.isRevoked()).isTrue();
        verify(repository).save(stored);
    }
}
