package com.bankingtest.serviceTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.banking.exception.DuplicateCustomerException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidEmailException;
import com.banking.model.AccountType;
import com.banking.model.Address;
import com.banking.model.BankAccount;
import com.banking.model.CurrentAccount;
import com.banking.model.Customer;
import com.banking.model.SavingsAccount;
import com.banking.model.Transaction;
import com.banking.service.BankingService;
import com.banking.util.FileLogger;
import com.banking.wallet.WalletOperations;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class BankingServiceTest {

    private static final double DELTA = 1e-9;

    private BankingService service;
    private Address address;

    @Before
    public void setUp() {
        // FileLogger only writes to SLF4J, so it is safe to use a real instance.
        service = new BankingService(new FileLogger());
        address = new Address("1 Main St", "Pune", "MH", "411001");
    }

    private Customer newCustomer() {
        return service.createCustomer("Asha", "asha@example.com", "9876543210", address);
    }

    // ---- customers ----

    @Test
    public void createCustomerRegistersCustomer() {
        Customer c = newCustomer();
        assertNotNull(c.getCustomerId());
        assertTrue(c.getCustomerId().startsWith("CUS-"));
        assertEquals(1, service.getCustomers().size());
        assertTrue(service.getCustomers().containsKey(c.getCustomerId()));
    }

    @Test(expected = DuplicateCustomerException.class)
    public void createCustomerRejectsDuplicateEmail() {
        newCustomer();
        service.createCustomer("Asha Copy", "ASHA@EXAMPLE.COM", "9876500000", address);
    }

    @Test(expected = InvalidEmailException.class)
    public void createCustomerRejectsInvalidEmail() {
        service.createCustomer("Bad", "not-an-email", "9876543210", address);
    }

    // ---- accounts ----

    @Test
    public void openSavingsAccountCreatesSavingsAccount() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 1_000.0);
        assertTrue(acc instanceof SavingsAccount);
        assertTrue(acc.getAccountNumber().startsWith("ACC-"));
        assertEquals(1_000.0, acc.getBalance(), DELTA);
        assertEquals(acc, service.getAccount(acc.getAccountNumber()));
    }

    @Test
    public void openCurrentAccountCreatesCurrentAccount() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.CURRENT, c, 500.0);
        assertTrue(acc instanceof CurrentAccount);
    }

    @Test(expected = IllegalArgumentException.class)
    public void openAccountRejectsUnknownCustomer() {
        Customer stranger = new Customer("CUS-X", "Stranger", "x@example.com", "9876543210", address);
        service.openAccount(AccountType.SAVINGS, stranger, 100.0);
    }


    @Test
    public void depositIncreasesBalanceAndRecordsTransaction() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 100.0);
        service.deposit(acc.getAccountNumber(), 400.0);
        assertEquals(500.0, service.getAccount(acc.getAccountNumber()).getBalance(), DELTA);
        assertEquals(1, service.viewTransactions(acc.getAccountNumber()).size());
    }

    @Test
    public void withdrawDecreasesBalance() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 1_000.0);
        service.withdraw(acc.getAccountNumber(), 600.0);
        assertEquals(400.0, service.getAccount(acc.getAccountNumber()).getBalance(), DELTA);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void withdrawRejectsInsufficientFunds() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 100.0);
        service.withdraw(acc.getAccountNumber(), 500.0);
    }

    @Test
    public void transferMovesMoneyBetweenAccounts() {
        Customer c = newCustomer();
        BankAccount from = service.openAccount(AccountType.SAVINGS, c, 1_000.0);
        BankAccount to = service.openAccount(AccountType.SAVINGS, c, 0.0);
        service.transfer(from.getAccountNumber(), to.getAccountNumber(), 300.0);
        assertEquals(700.0, service.getAccount(from.getAccountNumber()).getBalance(), DELTA);
        assertEquals(300.0, service.getAccount(to.getAccountNumber()).getBalance(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void operationsOnUnknownAccountThrow() {
        service.deposit("ACC-missing", 100.0);
    }

    @Test
    public void cloneAccountProducesIndependentCopy() throws CloneNotSupportedException {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 1_000.0);
        BankAccount copy = service.cloneAccount(acc.getAccountNumber());
        assertNotNull(copy);
        assertEquals(acc.getAccountNumber(), copy.getAccountNumber());
        assertEquals(acc.getBalance(), copy.getBalance(), DELTA);
    }


    @Test
    public void createPaytmWalletRegistersWallet() {
        Customer c = newCustomer();
        WalletOperations w = service.createPaytmWallet(c, 500.0);
        assertTrue(w.getWalletId().startsWith("PYTM-"));
        assertEquals(w, service.getWallet(w.getWalletId()));
    }

    @Test
    public void createPhonePeWalletRegistersWallet() {
        Customer c = newCustomer();
        WalletOperations w = service.createPhonePeWallet(c, 500.0);
        assertTrue(w.getWalletId().startsWith("PHPE-"));
    }

    @Test
    public void walletAddMoneyAndPayBillAdjustBalance() {
        Customer c = newCustomer();
        WalletOperations w = service.createPaytmWallet(c, 100.0);
        service.walletAddMoney(w.getWalletId(), 900.0);
        assertEquals(1_000.0, service.getWallet(w.getWalletId()).getBalance(), DELTA);
        service.walletPayBill(w.getWalletId(), 250.0);
        assertEquals(750.0, service.getWallet(w.getWalletId()).getBalance(), DELTA);
    }

    @Test
    public void walletTransferMovesMoney() {
        Customer c = newCustomer();
        WalletOperations from = service.createPaytmWallet(c, 1_000.0);
        WalletOperations to = service.createPhonePeWallet(c, 0.0);
        service.walletTransfer(from.getWalletId(), to.getWalletId(), 400.0);
        assertEquals(600.0, service.getWallet(from.getWalletId()).getBalance(), DELTA);
        assertEquals(400.0, service.getWallet(to.getWalletId()).getBalance(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void operationsOnUnknownWalletThrow() {
        service.walletAddMoney("PYTM-missing", 100.0);
    }

    @Test
    public void viewTransactionsReturnsRecordedHistory() {
        Customer c = newCustomer();
        BankAccount acc = service.openAccount(AccountType.SAVINGS, c, 100.0);
        service.deposit(acc.getAccountNumber(), 50.0);
        service.withdraw(acc.getAccountNumber(), 25.0);
        List<Transaction> txns = service.viewTransactions(acc.getAccountNumber());
        assertEquals(2, txns.size());
    }

    @Test
    public void gettersReturnImmutableSnapshots() {
        newCustomer();
        assertEquals(1, service.getCustomers().size());
        assertNotNull(service.getAccounts());
        assertNotNull(service.getWallets());
    }

    @Test
    public void serviceToleratesNullLogger() {
        BankingService noLogger = new BankingService(null);
        Customer c = noLogger.createCustomer("Ravi", "ravi@example.com", "9876500000", address);
        BankAccount acc = noLogger.openAccount(AccountType.SAVINGS, c, 100.0);
        noLogger.deposit(acc.getAccountNumber(), 50.0);
        assertEquals(150.0, noLogger.getAccount(acc.getAccountNumber()).getBalance(), DELTA);
    }
}
