package com.tnf.account.exception;

/**
 * Thrown when no account matches the given identifier. Mapped to HTTP 404.
 */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
