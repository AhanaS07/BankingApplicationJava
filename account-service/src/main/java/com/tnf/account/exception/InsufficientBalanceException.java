package com.tnf.account.exception;

/**
 * Thrown when a withdrawal or transfer would breach the account's balance rules
 * (savings minimum balance, or current-account overdraft limit). Mapped to HTTP 422.
 */
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
