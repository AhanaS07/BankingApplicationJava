package wallet;

import exception.InsufficientBalanceException;
import exception.InvalidAmountException;
import exception.WalletLimitExceededException;
import model.Customer;

import java.time.LocalDate;

abstract class AbstractWallet implements WalletOperations {

    private final String walletId;
    private final Customer linkedCustomer;
    private double balance;

    private LocalDate dailySpendDate;
    private double dailySpendTotal;

    protected AbstractWallet(String walletId, Customer linkedCustomer, double openingBalance) {
        if (openingBalance < 0 || openingBalance > MAX_BALANCE) {
            throw new InvalidAmountException("Invalid opening balance: " + openingBalance);
        }
        this.walletId = walletId;
        this.linkedCustomer = linkedCustomer;
        this.balance = openingBalance;
        this.dailySpendDate = LocalDate.now();
        this.dailySpendTotal = 0.0;
    }

    @Override
    public final void addMoney(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Top-up amount must be positive: " + amount);
        }
        if (balance + amount > MAX_BALANCE) {
            throw new WalletLimitExceededException(
                    "Wallet " + walletId + " would exceed MAX_BALANCE " + MAX_BALANCE);
        }
        balance += amount;
    }

    @Override
    public final void payBill(double amount) {
        debit(amount);
    }

    @Override
    public final void transferToWallet(WalletOperations target, double amount) {
        if (target == null) {
            throw new IllegalArgumentException("Target wallet cannot be null");
        }
        if (target == this) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }
        debit(amount);
        target.addMoney(amount);
    }

    private void debit(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Debit amount must be positive: " + amount);
        }
        rolloverDailyCounterIfNeeded();
        if (dailySpendTotal + amount > DAILY_LIMIT) {
            throw new WalletLimitExceededException(
                    "Wallet " + walletId + " daily spend limit (" + DAILY_LIMIT + ") would be exceeded");
        }
        if (balance < amount) {
            throw new InsufficientBalanceException(
                    "Wallet " + walletId + " has insufficient balance (" + balance + ")");
        }
        balance -= amount;
        dailySpendTotal += amount;
    }

    private void rolloverDailyCounterIfNeeded() {
        LocalDate today = LocalDate.now();
        if (!today.equals(dailySpendDate)) {
            dailySpendDate = today;
            dailySpendTotal = 0.0;
        }
    }

    @Override
    public String getWalletId() { return walletId; }

    @Override
    public double getBalance() { return balance; }

    public Customer getLinkedCustomer() { return linkedCustomer; }
}
