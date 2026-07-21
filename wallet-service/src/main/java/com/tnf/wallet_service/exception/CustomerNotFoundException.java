package com.tnf.wallet_service.exception;

/**
 * Thrown when the customer referenced by a wallet does not exist in customer-service.
 * Mapped to HTTP 404.
 */
public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(String message) {
        super(message);
    }
}
