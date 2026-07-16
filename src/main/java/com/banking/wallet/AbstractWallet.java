package com.banking.wallet;

import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidAmountException;
import com.banking.exception.WalletLimitExceededException;
import com.banking.model.Customer;

import java.math.BigDecimal;
import java.time.LocalDate;

abstract class AbstractWallet implements WalletOperations {

    private final String walletId;
    private final Customer linkedCustomer;
    private BigDecimal balance;

    private LocalDate dailySpendDate;
    private double dailySpendTotal;

    protected AbstractWallet(String walletId, Customer linkedCustomer, double openingBalance) {
        if (openingBalance < 0 || openingBalance > MAX_BALANCE) {
            throw new InvalidAmountException("Invalid opening balance: " + openingBalance);
        }
        this.walletId = walletId;
        this.linkedCustomer = linkedCustomer;
        this.balance = BigDecimal.valueOf(openingBalance);
        this.dailySpendDate = LocalDate.now();
        this.dailySpendTotal = 0.0;
    }

    @Override
    public final void addMoney(double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Top-up amount must be positive: " + amount);
        }
        if (balance.add(BigDecimal.valueOf(amount)).compareTo(BigDecimal.valueOf(MAX_BALANCE)) > 0) {
            throw new WalletLimitExceededException(
                    "Wallet " + walletId + " would exceed MAX_BALANCE " + MAX_BALANCE);
        }
        balance = balance.add(BigDecimal.valueOf(amount));
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
        if (balance.compareTo(BigDecimal.valueOf(amount)) < 0) {
            throw new InsufficientBalanceException(
                    "Wallet " + walletId + " has insufficient balance (" + balance + ")");
        }
        balance = balance.subtract(BigDecimal.valueOf(amount));
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
    public BigDecimal getBalance() { return balance; }

    public Customer getLinkedCustomer() { return linkedCustomer; }
}
