package com.banking.util;

import com.banking.model.Transaction;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileLogger.class);

    public FileLogger() {
    }

    public FileLogger(String logPath) {
    }

    public void logError(String message) throws IOException {
        LOGGER.error(message);
    }

    public void logTransaction(Transaction txn) throws IOException {
        LOGGER.info("[TXN] {}", txn);
    }
}
