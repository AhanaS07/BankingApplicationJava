package com.tnf.account.exception;

/**
 * Thrown when customer-service cannot be reached to verify a customer. The account is not created
 * (fail-closed). Mapped to HTTP 503.
 */
public class CustomerServiceUnavailableException extends RuntimeException {

    public CustomerServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
