package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.banking.exception.InvalidAmountException;
import com.banking.model.Transaction;
import com.banking.model.TransactionType;

import org.junit.Test;

public class TransactionTest {

    private static final double DELTA = 1e-9;

    @Test
    public void constructorStoresAllFields() {
        Transaction txn = new Transaction("TXN-1", 100.0, TransactionType.DEPOSIT, "SUCCESS");
        assertEquals("TXN-1", txn.getTransactionId());
        assertEquals(100.0, txn.getAmount(), DELTA);
        assertEquals(TransactionType.DEPOSIT, txn.getType());
        assertEquals("SUCCESS", txn.getStatus());
        assertNotNull(txn.getTimestamp());
    }

    @Test
    public void constructorDefaultsNullStatusToSuccess() {
        Transaction txn = new Transaction("TXN-2", 50.0, TransactionType.WITHDRAW, null);
        assertEquals("SUCCESS", txn.getStatus());
    }

    @Test(expected = InvalidAmountException.class)
    public void constructorRejectsZeroAmount() {
        new Transaction("TXN-3", 0.0, TransactionType.DEPOSIT, "SUCCESS");
    }

    @Test(expected = InvalidAmountException.class)
    public void constructorRejectsNegativeAmount() {
        new Transaction("TXN-4", -10.0, TransactionType.DEPOSIT, "SUCCESS");
    }

    @Test
    public void constructorRejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new Transaction(null, 10.0, TransactionType.DEPOSIT, "SUCCESS"));
    }

    @Test
    public void constructorRejectsNullType() {
        assertThrows(NullPointerException.class,
                () -> new Transaction("TXN-5", 10.0, null, "SUCCESS"));
    }

    @Test
    public void setStatusUpdatesStatus() {
        Transaction txn = new Transaction("TXN-6", 10.0, TransactionType.TRANSFER, "SUCCESS");
        txn.setStatus("FAILED");
        assertEquals("FAILED", txn.getStatus());
    }

    @Test
    public void toStringContainsKeyFields() {
        Transaction txn = new Transaction("TXN-7", 250.0, TransactionType.BILL, "SUCCESS");
        String text = txn.toString();
        assertTrue(text.startsWith("Transaction{"));
        assertTrue(text.contains("TXN-7"));
        assertTrue(text.contains("BILL"));
        assertTrue(text.contains("250.0"));
    }
}
