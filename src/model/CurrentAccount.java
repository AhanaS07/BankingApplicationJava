package model;

public class CurrentAccount extends BankAccount {

    private double overdraftLimit;

    public CurrentAccount(String accountNumber, Customer customer,
                          double initialBalance, double overdraftLimit) {
        super(accountNumber, customer, initialBalance);
        this.overdraftLimit = overdraftLimit;
    }

    public double getOverdraftLimit() { return overdraftLimit; }

    @Override
    public void displayDetails() {
        System.out.println("  --- Current Account ---");
        System.out.println("  Account No     : " + accountNumber);
        System.out.println("  Customer       : " + customer.getName());
        System.out.println("  Balance        : ₹" + balance);
        System.out.println("  Overdraft Limit: ₹" + overdraftLimit);
    }

    @Override
    public String toString() {
        return "CurrentAccount [" + accountNumber + " | " +
                customer.getName() + " | ₹" + balance + "]";
    }
}
