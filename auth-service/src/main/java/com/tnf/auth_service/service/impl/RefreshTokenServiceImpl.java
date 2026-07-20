package com.tnf.auth_service.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tnf.auth_service.config.JwtProperties;
import com.tnf.auth_service.entity.RefreshToken;
import com.tnf.auth_service.exception.RefreshTokenExpiredException;
import com.tnf.auth_service.exception.RefreshTokenNotFoundException;
import com.tnf.auth_service.repository.RefreshTokenRepository;
import com.tnf.auth_service.service.RefreshTokenService;

/**
 * Default {@link RefreshTokenService}. Tokens are 256 bits of URL-safe randomness, persisted so they
 * can be individually revoked and rotated.
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtProperties properties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.properties = properties;
    }

    @Override
    public RefreshToken create(String userId) {
        RefreshToken token = RefreshToken.builder()
                .token(generateTokenValue())
                .userId(userId)
                .expiryDate(Instant.now().plusMillis(properties.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        RefreshToken saved = refreshTokenRepository.save(token);
        log.debug("Issued refresh token for user {}", userId);
        return saved;
    }

    @Override
    public RefreshToken validate(String token) {
        RefreshToken stored = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not recognised"));
        if (stored.isRevoked()) {
            throw new RefreshTokenExpiredException("Refresh token has been revoked");
        }
        if (stored.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(stored);
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }
        return stored;
    }

    @Override
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(stored -> {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            log.debug("Revoked refresh token for user {}", stored.getUserId());
        });
    }

    @Override
    public void revokeAllForUser(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.debug("Deleted all refresh tokens for user {}", userId);
    }

    private String generateTokenValue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return encoder.encodeToString(bytes);
    }
}
