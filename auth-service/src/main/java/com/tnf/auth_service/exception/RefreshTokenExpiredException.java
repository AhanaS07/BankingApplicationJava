package com.tnf.auth_service.exception;

/**
 * Thrown when a presented refresh token has expired or been revoked and can no longer be exchanged.
 */
public class RefreshTokenExpiredException extends AuthException {

    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
