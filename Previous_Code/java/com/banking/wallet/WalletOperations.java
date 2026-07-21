package com.banking.wallet;

import java.math.BigDecimal;

public interface WalletOperations {

    double MAX_BALANCE = 50_000.0;
    double DAILY_LIMIT = 20_000.0;

    void addMoney(double amount);

    void payBill(double amount);

    void transferToWallet(WalletOperations target, double amount);

    String getWalletId();

    BigDecimal getBalance();
}
