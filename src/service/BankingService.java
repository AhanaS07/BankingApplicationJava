package service;

import exception.DuplicateCustomerException;
import model.AccountType;
import model.Address;
import model.BankAccount;
import model.CurrentAccount;
import model.Customer;
import model.SavingsAccount;
import model.Transaction;
import util.FileLogger;
import wallet.PaytmWallet;
import wallet.PhonePeWallet;
import wallet.WalletOperations;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BankingService {

    private static final double DEFAULT_SAVINGS_INTEREST = 3.5;
    private static final double DEFAULT_OVERDRAFT_LIMIT = 10_000.0;

    private final Map<String, Customer> customers = new HashMap<>();
    private final Map<String, BankAccount> accounts = new HashMap<>();
    private final Map<String, WalletOperations> wallets = new HashMap<>();
    private final FileLogger logger;

    public BankingService(FileLogger logger) {
        this.logger = logger;
    }

    public Customer createCustomer(String name, String email, String phoneNumber, Address address) {
        boolean exists = customers.values().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(email));
        if (exists) {
            DuplicateCustomerException ex = new DuplicateCustomerException(
                    "Customer with email already exists: " + email);
            safeLog(ex);
            throw ex;
        }
        String id = "CUS-" + UUID.randomUUID().toString().substring(0, 8);
        try {
            Customer customer = new Customer(id, name, email, phoneNumber, address);
            customers.put(id, customer);
            return customer;
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public BankAccount openAccount(AccountType type, Customer customer, double openingBalance) {
        if (!customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException("Unknown customer: " + customer.getCustomerId());
        }
        String accountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8);
        BankAccount account = switch (type) {
            case SAVINGS -> new SavingsAccount(accountNumber, customer, openingBalance, DEFAULT_SAVINGS_INTEREST);
            case CURRENT -> new CurrentAccount(accountNumber, customer, openingBalance, DEFAULT_OVERDRAFT_LIMIT);
        };
        accounts.put(accountNumber, account);
        return account;
    }

    public void deposit(String accountNumber, double amount) {
        BankAccount account = requireAccount(accountNumber);
        try {
            account.deposit(amount);
            logLastTransaction(account);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public void withdraw(String accountNumber, double amount) {
        BankAccount account = requireAccount(accountNumber);
        try {
            account.withdraw(amount);
            logLastTransaction(account);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public void transfer(String fromAccount, String toAccount, double amount) {
        BankAccount source = requireAccount(fromAccount);
        BankAccount target = requireAccount(toAccount);
        try {
            source.transfer(target, amount);
            logLastTransaction(source);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public BankAccount cloneAccount(String accountNumber) throws CloneNotSupportedException {
        return requireAccount(accountNumber).clone();
    }

    public WalletOperations createPaytmWallet(Customer customer, double openingBalance) {
        String walletId = "PYTM-" + UUID.randomUUID().toString().substring(0, 8);
        WalletOperations wallet = new PaytmWallet(walletId, customer, openingBalance);
        wallets.put(walletId, wallet);
        return wallet;
    }

    public WalletOperations createPhonePeWallet(Customer customer, double openingBalance) {
        String walletId = "PHPE-" + UUID.randomUUID().toString().substring(0, 8);
        WalletOperations wallet = new PhonePeWallet(walletId, customer, openingBalance);
        wallets.put(walletId, wallet);
        return wallet;
    }

    public void walletAddMoney(String walletId, double amount) {
        WalletOperations wallet = requireWallet(walletId);
        try {
            wallet.addMoney(amount);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public void walletPayBill(String walletId, double amount) {
        WalletOperations wallet = requireWallet(walletId);
        try {
            wallet.payBill(amount);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public void walletTransfer(String fromWalletId, String toWalletId, double amount) {
        WalletOperations from = requireWallet(fromWalletId);
        WalletOperations to = requireWallet(toWalletId);
        try {
            from.transferToWallet(to, amount);
        } catch (RuntimeException ex) {
            safeLog(ex);
            throw ex;
        }
    }

    public List<Transaction> viewTransactions(String accountNumber) {
        return requireAccount(accountNumber).getTransactions();
    }

    public BankAccount getAccount(String accountNumber) {
        return requireAccount(accountNumber);
    }

    public WalletOperations getWallet(String walletId) {
        return requireWallet(walletId);
    }

    public Map<String, Customer> getCustomers() { return Map.copyOf(customers); }
    public Map<String, BankAccount> getAccounts() { return Map.copyOf(accounts); }
    public Map<String, WalletOperations> getWallets() { return Map.copyOf(wallets); }

    private BankAccount requireAccount(String accountNumber) {
        BankAccount account = accounts.get(accountNumber);
        if (account == null) {
            throw new IllegalArgumentException("Account not found: " + accountNumber);
        }
        return account;
    }

    private WalletOperations requireWallet(String walletId) {
        WalletOperations wallet = wallets.get(walletId);
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet not found: " + walletId);
        }
        return wallet;
    }

    private void logLastTransaction(BankAccount account) {
        List<Transaction> txns = account.getTransactions();
        if (txns.isEmpty() || logger == null) return;
        try {
            logger.logTransaction(txns.get(txns.size() - 1));
        } catch (IOException ignored) {
        }
    }

    private void safeLog(Exception ex) {
        if (logger == null) return;
        try {
            logger.logError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } catch (IOException ignored) {
        }
    }
}
