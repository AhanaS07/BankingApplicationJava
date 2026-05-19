package wallet;

import exception.InvalidAmountException;
import exception.InsufficientBalanceException;
import exception.WalletLimitExceededException;
import model.Customer;

public class PaytmWallet implements WalletOperations {

    private static final double MAX_BALANCE     = 50000.0;
    private static final double DAILY_LIMIT     = 20000.0;

    private final Customer customer;
    private double balance;
    private double dailyTransferred;

    public PaytmWallet(Customer customer, double initialBalance)
            throws WalletLimitExceededException, InvalidAmountException {
        if (initialBalance < 0) {
            throw new InvalidAmountException("Initial balance cannot be negative");
        }
        if (initialBalance > MAX_BALANCE) {
            throw new WalletLimitExceededException("Initial balance exceeds wallet limit of ₹" + MAX_BALANCE);
        }
        this.customer = customer;
        this.balance = initialBalance;
        this.dailyTransferred = 0;
    }

    @Override
    public void addMoney(double amount) throws WalletLimitExceededException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than 0");
        }
        if (balance + amount > MAX_BALANCE) {
            throw new WalletLimitExceededException(
                "Adding ₹" + amount + " exceeds wallet limit of ₹" + MAX_BALANCE
            );
        }
        balance += amount;
        System.out.println("₹" + amount + " added to Paytm Wallet | Balance: ₹" + balance);
    }

    @Override
    public void payBill(String billName, double amount)
            throws InsufficientBalanceException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Bill amount must be greater than 0");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient wallet balance to pay " + billName);
        }
        balance -= amount;
        System.out.println("Bill paid: " + billName + " | ₹" + amount + " | Remaining: ₹" + balance);
    }

    @Override
    public void transferToWallet(WalletOperations target, double amount)
            throws InsufficientBalanceException, WalletLimitExceededException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Transfer amount must be greater than 0");
        }
        if (dailyTransferred + amount > DAILY_LIMIT) {
            throw new WalletLimitExceededException(
                "Daily transfer limit of ₹" + DAILY_LIMIT + " exceeded"
            );
        }
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient balance for transfer");
        }
        balance -= amount;
        dailyTransferred += amount;
        target.addMoney(amount);
        System.out.println("Transferred ₹" + amount + " | Paytm Balance: ₹" + balance);
    }

    @Override
    public double getBalance() { return balance; }

    @Override
    public void displayDetails() {
        System.out.println("--- Paytm Wallet ---");
        System.out.println("Owner  : " + customer.getName());
        System.out.println("Balance: ₹" + balance);
        System.out.println("Daily transferred: ₹" + dailyTransferred + " / ₹" + DAILY_LIMIT);
    }
}