package wallet;

import exception.InsufficientBalanceException;
import exception.InvalidAmountException;
import exception.WalletLimitExceededException;

public class PhonePeWallet implements WalletOperations {

    private static final double MAX_BALANCE          = 50000.0;
    private static final double DAILY_TRANSFER_LIMIT = 20000.0;

    private String walletId;
    private String ownerName;
    private double balance;
    private double dailyTransferredAmount;

    public PhonePeWallet(String walletId, String ownerName, double initialBalance) {
        this.walletId               = walletId;
        this.ownerName              = ownerName;
        this.balance                = initialBalance;
        this.dailyTransferredAmount = 0.0;
    }

    @Override
    public void addMoney(double amount)
            throws InvalidAmountException, WalletLimitExceededException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Amount to add must be greater than zero.");
        }
        if (balance + amount > MAX_BALANCE) {
            throw new WalletLimitExceededException(
                    "WalletLimitExceededException: Adding ₹" + amount +
                            " would exceed wallet limit of ₹" + MAX_BALANCE +
                            ". Current balance: ₹" + balance);
        }
        balance += amount;
        System.out.println("  [PhonePe] ₹" + amount + " added. Wallet balance: ₹" + balance);
    }

    @Override
    public void payBill(String billType, double amount)
            throws InvalidAmountException, InsufficientBalanceException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Bill payment amount must be greater than zero.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "InsufficientBalanceException: Insufficient PhonePe wallet balance to pay " +
                            billType + " bill. Available: ₹" + balance + ", Required: ₹" + amount);
        }
        balance -= amount;
        System.out.println("  [PhonePe] " + billType + " bill of ₹" + amount +
                " paid. Remaining balance: ₹" + balance);
    }

    @Override
    public void transferToWallet(WalletOperations targetWallet, double amount)
            throws InvalidAmountException, InsufficientBalanceException,
            WalletLimitExceededException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "InvalidAmountException: Transfer amount must be positive.");
        }
        if (dailyTransferredAmount + amount > DAILY_TRANSFER_LIMIT) {
            throw new WalletLimitExceededException(
                    "WalletLimitExceededException: Daily transfer limit of ₹" +
                            DAILY_TRANSFER_LIMIT + " exceeded.");
        }
        if (amount > balance) {
            throw new InsufficientBalanceException(
                    "InsufficientBalanceException: Insufficient PhonePe wallet balance. " +
                            "Available: ₹" + balance + ", Requested: ₹" + amount);
        }
        balance -= amount;
        dailyTransferredAmount += amount;
        targetWallet.addMoney(amount);
        System.out.println("  [PhonePe] Transferred ₹" + amount + " to target wallet.");
    }

    @Override
    public double getBalance() { return balance; }

    @Override
    public void displayBalance() {
        System.out.println("  [PhonePe Wallet] ID: " + walletId +
                " | Owner: " + ownerName +
                " | Balance: ₹" + balance);
    }
}
