package com.tnf.customer_service.exception;

/**
 * Thrown when a caller tries to access or modify a customer profile that is not their own
 * (per the gateway-injected X-Auth-Customer-Id header). Mapped to HTTP 403.
 */
public class UnauthorizedCustomerAccessException extends RuntimeException {

    public UnauthorizedCustomerAccessException(String message) {
        super(message);
    }
}
