package com.tnf.auth_service.exception;

/**
 * Thrown when authentication fails because the supplied credentials are wrong.
 */
public class InvalidCredentialsException extends AuthException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
