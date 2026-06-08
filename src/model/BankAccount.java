package model;

import exception.InsufficientBalanceException;
import exception.InvalidAmountException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class BankAccount implements Cloneable {

    protected final String accountNumber;
    protected Customer customer;
    protected double balance;
    protected List<Transaction> transactions;

    protected BankAccount(String accountNumber, Customer customer, double openingBalance) {
        if (openingBalance < 0) {
            throw new InvalidAmountException("Opening balance cannot be negative: " + openingBalance);
        }
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.balance = openingBalance;
        this.transactions = new ArrayList<>();
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive: " + amount);
        }
        this.balance += amount;
        recordTransaction(amount, TransactionType.DEPOSIT, "SUCCESS");
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive: " + amount);
        }
        ensureWithdrawAllowed(amount);
        this.balance -= amount;
        recordTransaction(amount, TransactionType.WITHDRAW, "SUCCESS");
    }

    public void transfer(BankAccount target, double amount) {
        if (target == null) {
            throw new IllegalArgumentException("Target account cannot be null");
        }
        if (target == this) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be positive: " + amount);
        }
        ensureWithdrawAllowed(amount);
        this.balance -= amount;
        target.balance += amount;
        this.recordTransaction(amount, TransactionType.TRANSFER, "SUCCESS");
        target.recordTransaction(amount, TransactionType.TRANSFER, "SUCCESS");
    }

    protected void ensureWithdrawAllowed(double amount) {
        if (balance < amount) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + accountNumber +
                            " (balance=" + balance + ", required=" + amount + ")");
        }
    }

    protected void recordTransaction(double amount, TransactionType type, String status) {
        transactions.add(new Transaction(UUID.randomUUID().toString(), amount, type, status));
    }

    public abstract void displayDetails();
    public abstract double calculateInterest();

    public String getAccountNumber() { return accountNumber; }
    public Customer getCustomer() { return customer; }
    public double getBalance() { return balance; }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    @Override
    public BankAccount clone() throws CloneNotSupportedException {
        BankAccount copy = (BankAccount) super.clone();
        copy.customer = this.customer.clone();
        copy.transactions = new ArrayList<>(this.transactions);
        return copy;
    }
}
