package com.tnf.wallet_service.exception;

/**
 * Thrown when customer-service cannot be reached to verify a customer. The wallet is not created
 * (fail-closed). Mapped to HTTP 503.
 */
public class CustomerServiceUnavailableException extends RuntimeException {

    public CustomerServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
