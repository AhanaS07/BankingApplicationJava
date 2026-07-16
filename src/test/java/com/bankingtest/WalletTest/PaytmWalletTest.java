package com.bankingtest.WalletTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.banking.model.Customer;
import com.banking.wallet.PaytmWallet;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

public class PaytmWalletTest {

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", null);
    }

    @Test
    public void constructorSetsIdAndBalance() {
        PaytmWallet w = new PaytmWallet("PYTM-1", customer, 500.0);
        assertEquals("PYTM-1", w.getWalletId());
        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(w.getBalance()));
    }

    @Test
    public void toStringContainsIdAndBalance() {
        PaytmWallet w = new PaytmWallet("PYTM-9", customer, 750.0);
        String text = w.toString();
        assertTrue(text.startsWith("PaytmWallet{"));
        assertTrue(text.contains("PYTM-9"));
        assertTrue(text.contains("750.0"));
    }
}
