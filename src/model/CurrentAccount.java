package model;

import exception.InvalidAmountException;
import exception.InsufficientBalanceException;

public class CurrentAccount extends BankAccount {

    private static final double OVERDRAFT_LIMIT = 10000.0;  // can go negative up to this

    public CurrentAccount(String accountNumber, Customer customer, double balance)
            throws InvalidAmountException {
        super(accountNumber, customer, balance);
        if (balance < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }
    }

    @Override
    public void deposit(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be greater than 0");
        }
        balance += amount;
        transactionHistory.add(new Transaction(amount, "DEPOSIT"));
        System.out.println("Deposited ₹" + amount + " | New Balance: ₹" + balance);
    }

    @Override
    public void withdraw(double amount) throws InvalidAmountException, InsufficientBalanceException {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be greater than 0");
        }
        if (balance - amount < -OVERDRAFT_LIMIT) {
            throw new InsufficientBalanceException(
                "Overdraft limit of ₹" + OVERDRAFT_LIMIT + " exceeded"
            );
        }
        balance -= amount;
        transactionHistory.add(new Transaction(amount, "WITHDRAWAL"));
        System.out.println("Withdrawn ₹" + amount + " | New Balance: ₹" + balance);
    }

    @Override
    public void displayDetails() {
        System.out.println("--- Current Account ---");
        super.displayDetails();
        System.out.println("Overdraft Limit: ₹" + OVERDRAFT_LIMIT);
    }
}