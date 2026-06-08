package com.banking.model;

public class SavingsAccount extends BankAccount {

    private final double interestRate;

    public SavingsAccount(String accountNumber, Customer customer, double openingBalance, double interestRate) {
        super(accountNumber, customer, openingBalance);
        this.interestRate = interestRate;
    }

    public double getInterestRate() { return interestRate; }

    @Override
    public void displayDetails() {
        System.out.println("[Savings] account=" + accountNumber +
                ", holder=" + customer.getName() +
                ", balance=" + balance +
                ", interestRate=" + interestRate + "%");
    }

    @Override
    public double calculateInterest() {
        return balance * interestRate / 100.0;
    }
}
