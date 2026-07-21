package com.banking.model;

import com.banking.exception.InsufficientBalanceException;

public class CurrentAccount extends BankAccount {

    private final double overdraftLimit;

    public CurrentAccount(String accountNumber, Customer customer, double openingBalance, double overdraftLimit) {
        super(accountNumber, customer, openingBalance);
        this.overdraftLimit = overdraftLimit;
    }

    public double getOverdraftLimit() { return overdraftLimit; }

    @Override
    protected void ensureWithdrawAllowed(double amount) {
        if (balance + overdraftLimit < amount) {
            throw new InsufficientBalanceException(
                    "Overdraft limit exceeded for account " + accountNumber +
                            " (balance=" + balance + ", overdraft=" + overdraftLimit +
                            ", required=" + amount + ")");
        }
    }

    @Override
    public void displayDetails() {
        System.out.println("[Current] account=" + accountNumber +
                ", holder=" + customer.getName() +
                ", balance=" + balance +
                ", overdraftLimit=" + overdraftLimit);
    }

    @Override
    public double calculateInterest() {
        return 0.0;
    }
}
