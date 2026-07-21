package com.banking.exception;

public class WalletLimitExceededException extends RuntimeException {
    public WalletLimitExceededException(String message) {
        super(message);
    }

    public WalletLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
