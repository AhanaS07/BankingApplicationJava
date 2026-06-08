package com.bankingtest.WalletTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.banking.model.Customer;
import com.banking.wallet.PhonePeWallet;

import org.junit.Before;
import org.junit.Test;

public class PhonePeWalletTest {

    private static final double DELTA = 1e-9;

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-2", "Ravi", "ravi@example.com", "9876500000", null);
    }

    @Test
    public void constructorSetsIdAndBalance() {
        PhonePeWallet w = new PhonePeWallet("PHPE-1", customer, 1_200.0);
        assertEquals("PHPE-1", w.getWalletId());
        assertEquals(1_200.0, w.getBalance(), DELTA);
    }

    @Test
    public void toStringContainsIdAndBalance() {
        PhonePeWallet w = new PhonePeWallet("PHPE-7", customer, 333.0);
        String text = w.toString();
        assertTrue(text.startsWith("PhonePeWallet{"));
        assertTrue(text.contains("PHPE-7"));
        assertTrue(text.contains("333.0"));
    }
}
