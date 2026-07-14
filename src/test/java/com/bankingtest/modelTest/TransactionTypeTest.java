package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;

import com.banking.model.TransactionType;

import org.junit.Test;

public class TransactionTypeTest {

    @Test
    public void valuesContainsAllConstants() {
        assertEquals(4, TransactionType.values().length);
    }

    @Test
    public void valueOfResolvesConstants() {
        assertEquals(TransactionType.DEPOSIT, TransactionType.valueOf("DEPOSIT"));
        assertEquals(TransactionType.WITHDRAW, TransactionType.valueOf("WITHDRAW"));
        assertEquals(TransactionType.TRANSFER, TransactionType.valueOf("TRANSFER"));
        assertEquals(TransactionType.BILL, TransactionType.valueOf("BILL"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfRejectsUnknownConstant() {
        TransactionType.valueOf("UNKNOWN");
    }
}
