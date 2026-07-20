package com.tnf.auth_service.exception;

/**
 * Base type for all auth-service domain exceptions, so the {@link GlobalExceptionHandler} can treat
 * them uniformly where appropriate.
 */
public class AuthException extends RuntimeException {

    public AuthException(String message) {
        super(message);
    }

    public AuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
