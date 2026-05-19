package model;

import exception.InvalidAmountException;
import exception.InsufficientBalanceException;
import java.util.ArrayList;
import java.util.List;

public abstract class BankAccount implements Cloneable {

    protected String accountNumber;
    protected Customer customer;
    protected double balance;
    protected List<Transaction> transactionHistory;

    public BankAccount(String accountNumber, Customer customer, double balance) {
        this.accountNumber = accountNumber;
        this.customer = customer;
        this.balance = balance;
        this.transactionHistory = new ArrayList<>();
    }

    // deposit and withdraw are abstract — subclasses define their own rules
    public abstract void deposit(double amount) throws InvalidAmountException;
    public abstract void withdraw(double amount) throws InvalidAmountException, InsufficientBalanceException;

    // transfer is common to all accounts so defined here
    public void transfer(BankAccount target, double amount)
            throws InvalidAmountException, InsufficientBalanceException {

        if (this.accountNumber.equals(target.accountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be positive");
        }

        this.withdraw(amount);
        target.deposit(amount);

        // log transaction on both sides
        Transaction debit  = new Transaction(amount, "TRANSFER OUT");
        Transaction credit = new Transaction(amount, "TRANSFER IN");
        this.transactionHistory.add(debit);
        target.transactionHistory.add(credit);
    }

    public void displayDetails() {
        System.out.println("Account No  : " + accountNumber);
        System.out.println("Owner       : " + customer.getName());
        System.out.println("Email       : " + customer.getEmailID());
        System.out.println("Phone       : " + customer.getPhoneNo());
        System.out.println("Balance     : " + balance);
    }

    public void displayTransactions() {
        if (transactionHistory.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        for (Transaction t : transactionHistory) {
            System.out.println(t.getType() + " | Amount: " + t.getAmount() + " | " + t.getTimestamp());
        }
    }

    // Getters
    public String getAccountNumber() { return accountNumber; }
    public Customer getCustomer()    { return customer; }
    public double getBalance()       { return balance; }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        BankAccount cloned = (BankAccount) super.clone();
        // deep clone the transaction list so original isn't affected
        cloned.transactionHistory = new ArrayList<>(this.transactionHistory);
        return cloned;
    }
}