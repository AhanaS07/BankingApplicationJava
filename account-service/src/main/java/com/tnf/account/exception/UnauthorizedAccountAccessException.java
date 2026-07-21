package com.tnf.account.exception;

/**
 * Thrown when a caller tries to create an account for a customerId that is not their own
 * (per the gateway-injected X-Auth-Customer-Id header). Mapped to HTTP 403.
 */
public class UnauthorizedAccountAccessException extends RuntimeException {

    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
