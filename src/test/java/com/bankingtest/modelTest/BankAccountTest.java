package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAmountException;
import com.banking.model.BankAccount;
import com.banking.model.Customer;
import com.banking.model.SavingsAccount;
import com.banking.model.Transaction;
import com.banking.model.TransactionType;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Exercises the abstract {@link BankAccount} base class through the concrete
 * {@link SavingsAccount} subclass, mirroring how AbstractWalletTest drives the
 * abstract wallet through PaytmWallet.
 */
public class BankAccountTest {

    private static final double DELTA = 1e-9;

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", null);
    }

    private BankAccount account(double openingBalance) {
        return new SavingsAccount("ACC-1", customer, openingBalance, 4.0);
    }

    @Test
    public void constructorStoresOpeningState() {
        BankAccount acc = account(1_000.0);
        assertEquals("ACC-1", acc.getAccountNumber());
        assertEquals(1_000.0, acc.getBalance(), DELTA);
        assertSame(customer, acc.getCustomer());
        assertTrue(acc.getTransactions().isEmpty());
    }

    @Test(expected = InvalidAmountException.class)
    public void constructorRejectsNegativeOpeningBalance() {
        account(-1.0);
    }

    @Test
    public void depositIncreasesBalanceAndRecordsTransaction() {
        BankAccount acc = account(100.0);
        acc.deposit(250.0);
        assertEquals(350.0, acc.getBalance(), DELTA);
        List<Transaction> txns = acc.getTransactions();
        assertEquals(1, txns.size());
        assertEquals(TransactionType.DEPOSIT, txns.get(0).getType());
    }

    @Test(expected = InvalidAmountException.class)
    public void depositRejectsZero() {
        account(100.0).deposit(0.0);
    }

    @Test(expected = InvalidAmountException.class)
    public void depositRejectsNegative() {
        account(100.0).deposit(-5.0);
    }

    @Test
    public void withdrawDecreasesBalanceAndRecordsTransaction() {
        BankAccount acc = account(1_000.0);
        acc.withdraw(400.0);
        assertEquals(600.0, acc.getBalance(), DELTA);
        assertEquals(TransactionType.WITHDRAW, acc.getTransactions().get(0).getType());
    }

    @Test(expected = InvalidAmountException.class)
    public void withdrawRejectsNonPositive() {
        account(1_000.0).withdraw(0.0);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void withdrawRejectsInsufficientBalance() {
        account(100.0).withdraw(500.0);
    }

    @Test
    public void transferMovesMoneyAndRecordsOnBothSides() {
        BankAccount from = new SavingsAccount("ACC-from", customer, 1_000.0, 4.0);
        BankAccount to = new SavingsAccount("ACC-to", customer, 200.0, 4.0);
        from.transfer(to, 300.0);
        assertEquals(700.0, from.getBalance(), DELTA);
        assertEquals(500.0, to.getBalance(), DELTA);
        assertEquals(1, from.getTransactions().size());
        assertEquals(1, to.getTransactions().size());
        assertEquals(TransactionType.TRANSFER, from.getTransactions().get(0).getType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferRejectsNullTarget() {
        account(1_000.0).transfer(null, 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferRejectsSameAccount() {
        BankAccount acc = account(1_000.0);
        acc.transfer(acc, 100.0);
    }

    @Test(expected = InvalidAmountException.class)
    public void transferRejectsNonPositiveAmount() {
        BankAccount from = new SavingsAccount("ACC-from", customer, 1_000.0, 4.0);
        BankAccount to = new SavingsAccount("ACC-to", customer, 0.0, 4.0);
        from.transfer(to, 0.0);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void transferRejectsInsufficientBalance() {
        BankAccount from = new SavingsAccount("ACC-from", customer, 100.0, 4.0);
        BankAccount to = new SavingsAccount("ACC-to", customer, 0.0, 4.0);
        from.transfer(to, 500.0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getTransactionsReturnsUnmodifiableView() {
        BankAccount acc = account(100.0);
        acc.deposit(50.0);
        acc.getTransactions().clear();
    }

    @Test
    public void cloneProducesIndependentCopy() throws CloneNotSupportedException {
        BankAccount acc = account(1_000.0);
        acc.deposit(100.0);
        BankAccount copy = acc.clone();

        assertEquals(acc.getAccountNumber(), copy.getAccountNumber());
        assertEquals(acc.getBalance(), copy.getBalance(), DELTA);
        assertNotSame(acc.getCustomer(), copy.getCustomer());
        assertNotSame(acc.getTransactions(), copy.getTransactions());
        assertEquals(acc.getTransactions().size(), copy.getTransactions().size());

        // Mutating the original's history must not affect the copy.
        acc.deposit(200.0);
        assertEquals(1, copy.getTransactions().size());
    }
}
