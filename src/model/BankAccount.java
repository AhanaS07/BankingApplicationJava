package model;

import exception.InsufficientBalanceException;
import exception.InvalidAmountException;

import java.util.ArrayList;
import java.util.List;

public abstract class BankAccount implements Cloneable {

    protected String accountNumber;
    protected Customer customer;
    protected double balance;
    protected List<Transaction> transactionHistory;

    public BankAccount(String accountNumber, Customer customer, double initialBalance) {
        this.accountNumber      = accountNumber;
        this.customer           = customer;
        this.balance            = initialBalance;
        this.transactionHistory = new ArrayList<>();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public Customer getCustomer() {
        return customer;
    }

    public List<Transaction> getTransactionHistory() {
        return transactionHistory;
    }

    // deposit
    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Deposit amount cannot be negative or zero. Provided: " + amount);
        }
        balance += amount;
        transactionHistory.add(new Transaction(generateTxnId(), amount, "DEPOSIT"));
        System.out.println("  Deposited ₹" + amount + " successfully. New balance: ₹" + balance);
    }

    // withdraw
    public void withdraw(double amount)
            throws InvalidAmountException, InsufficientBalanceException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Withdraw amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "InsufficientBalanceException: Insufficient balance. Available: ₹" +
                            balance + ", Requested: ₹" + amount);
        }
        balance -= amount;
        transactionHistory.add(new Transaction(generateTxnId(), amount, "WITHDRAW"));
        System.out.println("  Withdrawn ₹" + amount + " successfully. New balance: ₹" + balance);
    }

    // transfer
    public void transfer(BankAccount target, double amount)
            throws InvalidAmountException, InsufficientBalanceException {
        // Business rule: sender and receiver cannot be the same account
        if (this.accountNumber.equals(target.getAccountNumber())) {
            throw new IllegalArgumentException(
                    "IllegalArgumentException: Transfer to the same account is not allowed.");
        }
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Transfer amount must be positive.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "InsufficientBalanceException: Insufficient balance for transfer. Available: ₹" +
                            balance + ", Requested: ₹" + amount);
        }
        balance -= amount;
        target.balance += amount;

        String txnId = generateTxnId();
        transactionHistory.add(new Transaction(txnId, amount, "TRANSFER-OUT"));
        target.transactionHistory.add(new Transaction(txnId, amount, "TRANSFER-IN"));

        System.out.println("  Transferred ₹" + amount +
                " from Account " + this.accountNumber +
                " to Account " + target.getAccountNumber());
    }

    // displayDetails (overridden in subclass)
    public abstract void displayDetails();

    // deep clone
    @Override
    public BankAccount clone() throws CloneNotSupportedException {
        BankAccount cloned = (BankAccount) super.clone();
        // Deep-copy the customer so changes to clone don't affect original
        cloned.customer = this.customer.clone();
        // Deep-copy transaction list
        cloned.transactionHistory = new ArrayList<>(this.transactionHistory);
        return cloned;
    }

    // helper
    private String generateTxnId() {
        return "TXN" + System.currentTimeMillis();
    }
}
