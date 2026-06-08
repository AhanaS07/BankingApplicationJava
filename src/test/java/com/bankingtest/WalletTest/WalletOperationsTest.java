package com.bankingtest.WalletTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.banking.wallet.WalletOperations;

import org.junit.Test;

public class WalletOperationsTest {

    private static final double DELTA = 1e-9;

    @Test
    public void maxBalanceConstantHasExpectedValue() {
        assertEquals(50_000.0, WalletOperations.MAX_BALANCE, DELTA);
    }

    @Test
    public void dailyLimitConstantHasExpectedValue() {
        assertEquals(20_000.0, WalletOperations.DAILY_LIMIT, DELTA);
    }

    @Test
    public void dailyLimitIsBelowMaxBalance() {
        assertTrue(WalletOperations.DAILY_LIMIT < WalletOperations.MAX_BALANCE);
    }
}
