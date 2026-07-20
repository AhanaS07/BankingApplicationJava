package com.tnf.auth_service.exception;

/**
 * Thrown when registration is attempted with a username or email that is already taken.
 */
public class UserAlreadyExistsException extends AuthException {

    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
