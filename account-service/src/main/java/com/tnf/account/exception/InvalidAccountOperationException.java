package com.tnf.account.exception;

/**
 * Thrown for operations that are structurally invalid regardless of balance — e.g. an initial
 * deposit below the required savings minimum, or transferring to the same account.
 * Mapped to HTTP 400.
 */
public class InvalidAccountOperationException extends RuntimeException {

    public InvalidAccountOperationException(String message) {
        super(message);
    }
}
