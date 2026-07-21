package com.tnf.wallet_service.exception;

/**
 * Thrown when a caller tries to create a wallet for a customerId that is not their own
 * (per the gateway-injected X-Auth-Customer-Id header). Mapped to HTTP 403.
 */
public class UnauthorizedWalletAccessException extends RuntimeException {

    public UnauthorizedWalletAccessException(String message) {
        super(message);
    }
}
