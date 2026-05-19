package model;

public class SavingsAccount extends BankAccount {

    private double interestRate;

    public SavingsAccount(String accountNumber, Customer customer,
                          double initialBalance, double interestRate) {
        super(accountNumber, customer, initialBalance);
        this.interestRate = interestRate;
    }

    public double getInterestRate() { return interestRate; }

    @Override
    public void displayDetails() {
        System.out.println("  --- Savings Account ---");
        System.out.println("  Account No   : " + accountNumber);
        System.out.println("  Customer     : " + customer.getName());
        System.out.println("  Balance      : ₹" + balance);
        System.out.println("  Interest Rate: " + interestRate + "%");
    }

    @Override
    public String toString() {
        return "SavingsAccount [" + accountNumber + " | " +
                customer.getName() + " | ₹" + balance + "]";
    }
}
