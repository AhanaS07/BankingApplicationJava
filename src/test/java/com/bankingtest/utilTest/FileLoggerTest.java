package com.bankingtest.utilTest;

import com.banking.model.Transaction;
import com.banking.model.TransactionType;
import com.banking.util.FileLogger;

import java.io.IOException;

import org.junit.Test;

public class FileLoggerTest {

    @Test
    public void defaultConstructorCreatesLogger() {
        FileLogger logger = new FileLogger();
        org.junit.Assert.assertNotNull(logger);
    }

    @Test
    public void pathConstructorCreatesLogger() {
        FileLogger logger = new FileLogger("some/path.log");
        org.junit.Assert.assertNotNull(logger);
    }

    @Test
    public void logErrorDoesNotThrow() throws IOException {
        new FileLogger().logError("something went wrong");
    }

    @Test
    public void logTransactionDoesNotThrow() throws IOException {
        Transaction txn = new Transaction("TXN-1", 100.0, TransactionType.DEPOSIT, "SUCCESS");
        new FileLogger().logTransaction(txn);
    }
}
