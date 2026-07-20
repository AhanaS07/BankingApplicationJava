package com.tnf.auth_service.exception;

/**
 * Thrown when a presented refresh token does not exist in the store.
 */
public class RefreshTokenNotFoundException extends AuthException {

    public RefreshTokenNotFoundException(String message) {
        super(message);
    }
}
