package com.bankingtest.utilTest;

import static org.junit.Assert.assertNotNull;

import com.banking.model.Transaction;
import com.banking.model.TransactionType;
import com.banking.util.FileLogger;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class FileLoggerTest {

    private Transaction transaction;

    @Before
    public void setUp() {
        transaction = new Transaction("TXN-1", 100.0, TransactionType.DEPOSIT, "SUCCESS");
    }

    @Test
    public void defaultConstructorCreatesLogger() {
        assertNotNull(new FileLogger());
    }

    @Test
    public void pathConstructorCreatesLogger() {
        assertNotNull(new FileLogger("some/path.log"));
    }

    @Test
    public void pathConstructorAcceptsNullPath() {
        assertNotNull(new FileLogger(null));
    }

    @Test
    public void logErrorDoesNotThrow() throws IOException {
        new FileLogger().logError("something went wrong");
    }

    @Test
    public void logErrorAcceptsNullMessage() throws IOException {
        new FileLogger().logError(null);
    }

    @Test
    public void logTransactionDoesNotThrow() throws IOException {
        new FileLogger().logTransaction(transaction);
    }

    @Test
    public void logTransactionAcceptsNullTransaction() throws IOException {
        new FileLogger().logTransaction(null);
    }
}
