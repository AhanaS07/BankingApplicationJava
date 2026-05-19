package model;

import exception.InvalidAmountException;
import exception.InsufficientBalanceException;

public class SavingsAccount extends BankAccount {

    private static final double MIN_BALANCE = 500.0;

    public SavingsAccount(String accountNumber, Customer customer, double balance)
            throws InvalidAmountException {
        super(accountNumber, customer, balance);
        if (balance < MIN_BALANCE) {
            throw new InvalidAmountException("Savings account requires minimum balance of ₹" + MIN_BALANCE);
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
        if (balance - amount < MIN_BALANCE) {
            throw new InsufficientBalanceException(
                "Cannot withdraw. Minimum balance of ₹" + MIN_BALANCE + " must be maintained"
            );
        }
        balance -= amount;
        transactionHistory.add(new Transaction(amount, "WITHDRAWAL"));
        System.out.println("Withdrawn ₹" + amount + " | New Balance: ₹" + balance);
    }

    @Override
    public void displayDetails() {
        System.out.println("--- Savings Account ---");
        super.displayDetails();
        System.out.println("Minimum Balance: ₹" + MIN_BALANCE);
    }
}