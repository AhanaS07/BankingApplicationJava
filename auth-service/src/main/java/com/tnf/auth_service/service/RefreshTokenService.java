package com.tnf.auth_service.service;

import com.tnf.auth_service.entity.RefreshToken;

/**
 * Manages the lifecycle of persisted refresh tokens: creation, validation, rotation and revocation.
 */
public interface RefreshTokenService {

    /** Issues and stores a fresh refresh token for the given user. */
    RefreshToken create(String userId);

    /**
     * Loads a refresh token and asserts it is usable.
     *
     * @throws com.tnf.auth_service.exception.RefreshTokenNotFoundException if unknown
     * @throws com.tnf.auth_service.exception.RefreshTokenExpiredException  if expired or revoked
     */
    RefreshToken validate(String token);

    /** Marks a single refresh token as revoked (idempotent). */
    void revoke(String token);

    /** Revokes every refresh token belonging to a user. */
    void revokeAllForUser(String userId);
}
