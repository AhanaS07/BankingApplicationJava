package com.tnf.auth_service.exception;

/**
 * Thrown when a user lookup (by id, username or email) yields no result.
 */
public class UserNotFoundException extends AuthException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
