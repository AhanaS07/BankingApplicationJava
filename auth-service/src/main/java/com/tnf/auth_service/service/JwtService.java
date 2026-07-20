package com.tnf.auth_service.service;

import java.util.Set;

import com.tnf.auth_service.entity.User;

/**
 * Issues and inspects stateless JWT access tokens.
 */
public interface JwtService {

    /** Issues a signed access token carrying the user's id and roles. */
    String generateAccessToken(User user);

    /** Returns {@code true} if the token's signature is valid and it has not expired. */
    boolean isTokenValid(String token);

    /** Extracts the subject (username) claim; throws if the token is malformed or expired. */
    String extractUsername(String token);

    /** Extracts the {@code roles} claim. */
    Set<String> extractRoles(String token);

    /** Access-token lifetime in milliseconds, for populating {@code expiresIn}-style fields. */
    long getAccessTokenExpirationMs();
}
