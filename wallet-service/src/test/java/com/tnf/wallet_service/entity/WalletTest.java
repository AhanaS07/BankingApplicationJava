package com.tnf.wallet_service.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/*
Accessors are tested end-to-end by the service and controller tests, so this
covers only: positional constructor Mongo's mapper uses when materialising
a document, and {@code toString()}, which is what actually reaches the log files.
 */
class WalletTest {

    @Test
    @DisplayName("the positional constructor maps arguments onto the right fields")
    void allArgsConstructorAssignsEveryFieldInOrder() {
        LocalDate spendDate = LocalDate.of(2026, 7, 31);

        Wallet wallet = new Wallet("W1", "cust-1", WalletType.PHONEPE,
                new BigDecimal("250.50"), spendDate, new BigDecimal("40"));

        assertEquals("W1", wallet.getWalletId());
        assertEquals("cust-1", wallet.getCustomerId());
        assertEquals(WalletType.PHONEPE, wallet.getWalletType());
        assertEquals(new BigDecimal("250.50"), wallet.getBalance());
        assertEquals(spendDate, wallet.getDailySpendDate());
        assertEquals(new BigDecimal("40"), wallet.getDailySpendTotal());
    }

    @Test
    @DisplayName("the no-arg constructor leaves every field unset for the mapper to populate")
    void noArgsConstructorLeavesFieldsNull() {
        Wallet wallet = new Wallet();

        assertNull(wallet.getWalletId());
        assertNull(wallet.getCustomerId());
        assertNull(wallet.getWalletType());
        assertNull(wallet.getBalance());
        assertNull(wallet.getDailySpendDate());
        assertNull(wallet.getDailySpendTotal());
    }

    @Test
    @DisplayName("toString reports the state needed to debug a balance discrepancy")
    void toStringIncludesBalanceAndDailySpendState() {
        Wallet wallet = new Wallet("W1", "cust-1", WalletType.PAYTM,
                new BigDecimal("100"), LocalDate.of(2026, 7, 31), new BigDecimal("40"));

        String rendered = wallet.toString();

        assertTrue(rendered.contains("walletId=W1"), rendered);
        assertTrue(rendered.contains("customerId=cust-1"), rendered);
        assertTrue(rendered.contains("walletType=PAYTM"), rendered);
        assertTrue(rendered.contains("balance=100"), rendered);
        assertTrue(rendered.contains("dailySpendDate=2026-07-31"), rendered);
        assertTrue(rendered.contains("dailySpendTotal=40"), rendered);
    }

    @Test
    @DisplayName("setters replace state, including clearing it back to null")
    void settersReplaceState() {
        Wallet wallet = new Wallet("W1", "cust-1", WalletType.PAYTM,
                BigDecimal.TEN, LocalDate.of(2026, 7, 31), BigDecimal.ZERO);

        wallet.setWalletId("W2");
        wallet.setCustomerId("cust-2");
        wallet.setWalletType(null);
        wallet.setBalance(new BigDecimal("99"));
        wallet.setDailySpendDate(LocalDate.of(2026, 8, 1));
        wallet.setDailySpendTotal(new BigDecimal("5"));

        assertEquals("W2", wallet.getWalletId());
        assertEquals("cust-2", wallet.getCustomerId());
        assertNull(wallet.getWalletType());
        assertEquals(new BigDecimal("99"), wallet.getBalance());
        assertEquals(LocalDate.of(2026, 8, 1), wallet.getDailySpendDate());
        assertEquals(new BigDecimal("5"), wallet.getDailySpendTotal());
        assertFalse(wallet.toString().contains("walletId=W1"));
    }
    
    @Test
    @DisplayName("the enum constants still match the walletType pattern accepted by the API")
    void walletTypeConstantsMatchTheDtoValidationPattern() {
        assertEquals(Arrays.asList("PAYTM", "PHONEPE"),
                Arrays.stream(WalletType.values()).map(Enum::name).toList());
        assertEquals(WalletType.PAYTM, WalletType.valueOf("PAYTM"));
        assertEquals(WalletType.PHONEPE, WalletType.valueOf("PHONEPE"));
    }
}
