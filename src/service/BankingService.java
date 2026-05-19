package service;

import exception.*;
import model.*;
import util.FileLogger;
import wallet.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankingService {

    // In-memory stores
    private Map<String, Customer>    customers    = new HashMap<>();
    private Map<String, BankAccount> accounts     = new HashMap<>();
    private List<WalletOperations>   wallets      = new ArrayList<>();

    // =========================================================== Customer Module

    public void createCustomer(String customerId, String name,
                               String email, String phoneNumber)
            throws DuplicateCustomerException, InvalidEmailException,
            InvalidPhoneNumberException {

        // Check for duplicate customer ID
        if (customers.containsKey(customerId)) {
            throw new DuplicateCustomerException(
                    "DuplicateCustomerException: Customer ID '" + customerId + "' already exists.");
        }
        // Validate email
        if (email == null || !email.contains("@")) {
            throw new InvalidEmailException(
                    "InvalidEmailException: Email must contain '@'. Provided: " + email);
        }
        // Validate phone (exactly 10 digits)
        if (phoneNumber == null || !phoneNumber.matches("\\d{10}")) {
            throw new InvalidPhoneNumberException(
                    "InvalidPhoneNumberException: Phone number must be exactly 10 digits. " +
                            "Provided: " + phoneNumber);
        }

        Customer customer = new Customer(customerId, name, email, phoneNumber);
        customers.put(customerId, customer);
        System.out.println("  Customer created: " + customer);
        FileLogger.logInfo("Customer created: " + customerId + " | " + name);
    }

    public Customer getCustomer(String customerId) {
        Customer c = customers.get(customerId);
        if (c == null) {
            // Unchecked: NullPointerException scenario — we throw IllegalArgumentException
            throw new IllegalArgumentException(
                    "No customer found with ID: " + customerId);
        }
        return c;
    }

    public void listCustomers() {
        if (customers.isEmpty()) {
            System.out.println("  No customers registered.");
            return;
        }
        customers.values().forEach(c -> System.out.println("  " + c));
    }

    // =========================================================== Account Module

    public void openSavingsAccount(String accountNumber, String customerId,
                                   double initialBalance, double interestRate)
            throws InvalidAmountException {
        Customer customer = getCustomer(customerId);
        if (initialBalance < 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Initial balance cannot be negative.");
        }
        SavingsAccount account = new SavingsAccount(accountNumber, customer,
                initialBalance, interestRate);
        accounts.put(accountNumber, account);
        System.out.println("  Savings account opened: " + accountNumber);
        FileLogger.logInfo("Savings account opened: " + accountNumber + " for " + customerId);
    }

    public void openCurrentAccount(String accountNumber, String customerId,
                                   double initialBalance, double overdraftLimit)
            throws InvalidAmountException {
        Customer customer = getCustomer(customerId);
        if (initialBalance < 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Initial balance cannot be negative.");
        }
        CurrentAccount account = new CurrentAccount(accountNumber, customer,
                initialBalance, overdraftLimit);
        accounts.put(accountNumber, account);
        System.out.println("  Current account opened: " + accountNumber);
        FileLogger.logInfo("Current account opened: " + accountNumber + " for " + customerId);
    }

    public BankAccount getAccount(String accountNumber) {
        BankAccount acc = accounts.get(accountNumber);
        if (acc == null) {
            throw new IllegalArgumentException(
                    "No account found with number: " + accountNumber);
        }
        return acc;
    }

    public void listAccounts() {
        if (accounts.isEmpty()) {
            System.out.println("  No accounts found.");
            return;
        }
        accounts.values().forEach(BankAccount::displayDetails);
    }

    // =========================================================== Deposit

    public void deposit(String accountNumber, double amount) {
        try {
            BankAccount account = getAccount(accountNumber);
            account.deposit(amount);
            FileLogger.logInfo("Deposit of ₹" + amount + " to account " + accountNumber);
        } catch (InvalidAmountException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } finally {
            System.out.println("  [Deposit operation completed]");
        }
    }

    // =========================================================== Withdraw

    public void withdraw(String accountNumber, double amount) {
        try {
            BankAccount account = getAccount(accountNumber);
            account.withdraw(amount);
            FileLogger.logInfo("Withdrawal of ₹" + amount + " from account " + accountNumber);
        } catch (InvalidAmountException | InsufficientBalanceException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } finally {
            System.out.println("  [Withdraw operation completed]");
        }
    }

    // =========================================================== Transfer

    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        try {
            BankAccount from = getAccount(fromAccountNumber);
            BankAccount to   = getAccount(toAccountNumber);
            from.transfer(to, amount);
            FileLogger.logInfo("Transfer of ₹" + amount +
                    " from " + fromAccountNumber + " to " + toAccountNumber);
        } catch (InvalidAmountException | InsufficientBalanceException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
            FileLogger.logError(e.getMessage());
        } finally {
            System.out.println("  [Transfer operation completed]");
        }
    }

    // =========================================================== View Transactions

    public void viewTransactions(String accountNumber) {
        try {
            BankAccount account = getAccount(accountNumber);
            List<Transaction> history = account.getTransactionHistory();
            if (history.isEmpty()) {
                System.out.println("  No transactions found for account " + accountNumber);
            } else {
                System.out.println("  --- Transaction History for " + accountNumber + " ---");
                for (Transaction t : history) {
                    System.out.println("  " + t);
                }
            }
        } catch (IllegalArgumentException e) {
            System.out.println("  [EXCEPTION] " + e.getMessage());
        }
    }

    // =========================================================== Wallet Module

    public PaytmWallet createPaytmWallet(String walletId, String ownerName,
                                         double initialBalance)
            throws WalletLimitExceededException, InvalidAmountException {
        if (initialBalance < 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Wallet balance cannot be negative.");
        }
        if (initialBalance > 50000) {
            throw new WalletLimitExceededException(
                    "WalletLimitExceededException: Initial balance exceeds ₹50,000 limit.");
        }
        PaytmWallet wallet = new PaytmWallet(walletId, ownerName, initialBalance);
        wallets.add(wallet);
        System.out.println("  Paytm wallet created for " + ownerName);
        return wallet;
    }

    public PhonePeWallet createPhonePeWallet(String walletId, String ownerName,
                                             double initialBalance)
            throws WalletLimitExceededException, InvalidAmountException {
        if (initialBalance < 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Wallet balance cannot be negative.");
        }
        if (initialBalance > 50000) {
            throw new WalletLimitExceededException(
                    "WalletLimitExceededException: Initial balance exceeds ₹50,000 limit.");
        }
        PhonePeWallet wallet = new PhonePeWallet(walletId, ownerName, initialBalance);
        wallets.add(wallet);
        System.out.println("  PhonePe wallet created for " + ownerName);
        return wallet;
    }

    // =========================================================== Cloning

    public void demonstrateShallowVsDeepClone(String customerId)
            throws CloneNotSupportedException {
        Customer original = getCustomer(customerId);

        System.out.println("\n  --- Shallow Copy Issue Demonstration ---");
        // Shallow copy: both references point to same String objects
        Customer shallowCopy = original;   // not a real clone — same reference
        System.out.println("  Original   : " + original);
        System.out.println("  Shallow ref: " + shallowCopy);
        shallowCopy.setName("MODIFIED NAME (shallow)");
        System.out.println("  After modifying shallowCopy.name:");
        System.out.println("  Original name is ALSO changed: " + original.getName());

        // Restore original name
        original.setName(customerId.equals(original.getCustomerId()) ?
                original.getName() : original.getName());
        // reset for demo clarity
        original.setName("Original_" + customerId);

        System.out.println("\n  --- Deep Clone Solution ---");
        Customer deepClone = original.clone();   // throws CloneNotSupportedException
        System.out.println("  Original  : " + original);
        System.out.println("  Deep Clone: " + deepClone);
        deepClone.setName("MODIFIED NAME (deep clone)");
        System.out.println("  After modifying deepClone.name:");
        System.out.println("  Original name is UNCHANGED: " + original.getName());
        System.out.println("  Deep clone name            : " + deepClone.getName());
    }

    public BankAccount cloneAccount(String accountNumber) throws CloneNotSupportedException {
        BankAccount original = getAccount(accountNumber);
        BankAccount cloned   = original.clone();
        System.out.println("  Account cloned from: " + accountNumber);
        System.out.println("  Original customer  : " + original.getCustomer().getName());
        System.out.println("  Cloned  customer   : " + cloned.getCustomer().getName());
        return cloned;
    }
}
