package com.bankingtest.WalletTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAmountException;
import com.banking.exception.WalletLimitExceededException;
import com.banking.model.Customer;
import com.banking.wallet.PaytmWallet;
import com.banking.wallet.WalletOperations;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;


public class AbstractWalletTest {

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", null);
    }

    private PaytmWallet wallet(double openingBalance) {
        return new PaytmWallet("W-1", customer, openingBalance);
    }

    @Test
    public void constructorStoresOpeningState() {
        PaytmWallet w = wallet(1_000.0);
        assertEquals("W-1", w.getWalletId());
        assertEquals(0, BigDecimal.valueOf(1_000.0).compareTo(w.getBalance()));
        assertEquals(customer, w.getLinkedCustomer());
    }

    @Test
    public void constructorAcceptsZeroAndMaxOpeningBalance() {
        assertEquals(0, BigDecimal.ZERO.compareTo(wallet(0.0).getBalance()));
        assertEquals(0, BigDecimal.valueOf(WalletOperations.MAX_BALANCE)
                .compareTo(wallet(WalletOperations.MAX_BALANCE).getBalance()));
    }

    @Test(expected = InvalidAmountException.class)
    public void constructorRejectsNegativeOpeningBalance() {
        wallet(-1.0);
    }

    @Test(expected = InvalidAmountException.class)
    public void constructorRejectsOpeningBalanceAboveMax() {
        wallet(WalletOperations.MAX_BALANCE + 0.01);
    }

    @Test
    public void addMoneyIncreasesBalance() {
        PaytmWallet w = wallet(100.0);
        w.addMoney(250.0);
        assertEquals(0, BigDecimal.valueOf(350.0).compareTo(w.getBalance()));
    }

    @Test(expected = InvalidAmountException.class)
    public void addMoneyRejectsZero() {
        wallet(100.0).addMoney(0.0);
    }

    @Test(expected = InvalidAmountException.class)
    public void addMoneyRejectsNegative() {
        wallet(100.0).addMoney(-50.0);
    }

    @Test(expected = WalletLimitExceededException.class)
    public void addMoneyRejectsExceedingMaxBalance() {
        PaytmWallet w = wallet(WalletOperations.MAX_BALANCE);
        w.addMoney(1.0);
    }

    @Test
    public void payBillDebitsBalance() {
        PaytmWallet w = wallet(1_000.0);
        w.payBill(400.0);
        assertEquals(0, BigDecimal.valueOf(600.0).compareTo(w.getBalance()));
    }

    @Test(expected = InvalidAmountException.class)
    public void payBillRejectsNonPositive() {
        wallet(1_000.0).payBill(0.0);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void payBillRejectsInsufficientBalance() {
        PaytmWallet w = wallet(100.0);
        w.payBill(500.0);
    }

    @Test(expected = WalletLimitExceededException.class)
    public void payBillRejectsExceedingDailyLimit() {
        // Fund the wallet above the daily limit, then attempt to spend past it.
        PaytmWallet w = wallet(WalletOperations.MAX_BALANCE);
        w.payBill(WalletOperations.DAILY_LIMIT);
        w.payBill(1.0);
    }

    @Test
    public void transferMovesMoneyBetweenWallets() {
        PaytmWallet from = new PaytmWallet("W-from", customer, 1_000.0);
        PaytmWallet to = new PaytmWallet("W-to", customer, 200.0);
        from.transferToWallet(to, 300.0);
        assertEquals(0, BigDecimal.valueOf(700.0).compareTo(from.getBalance()));
        assertEquals(0, BigDecimal.valueOf(500.0).compareTo(to.getBalance()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferRejectsNullTarget() {
        wallet(1_000.0).transferToWallet(null, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferRejectsSameWallet() {
        PaytmWallet w = wallet(1_000.0);
        w.transferToWallet(w, 100.0);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void transferRejectsInsufficientBalance() {
        PaytmWallet from = new PaytmWallet("W-from", customer, 100.0);
        PaytmWallet to = new PaytmWallet("W-to", customer, 0.0);
        from.transferToWallet(to, 500.0);
    }

    @Test
    public void linkedCustomerIsExposed() {
        assertNotNull(wallet(0.0).getLinkedCustomer());
    }
}
