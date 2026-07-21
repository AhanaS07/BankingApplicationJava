package com.tnf.account.exception;

/**
 * Thrown when the customer referenced by an account does not exist in customer-service.
 * Mapped to HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }

    public CustomerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
