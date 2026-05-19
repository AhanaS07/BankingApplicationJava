package wallet;

import exception.InsufficientBalanceException;
import exception.InvalidAmountException;
import exception.WalletLimitExceededException;

public interface WalletOperations {

    void addMoney(double amount)
            throws InvalidAmountException, WalletLimitExceededException;

    void payBill(String billType, double amount)
            throws InvalidAmountException, InsufficientBalanceException;

    void transferToWallet(WalletOperations targetWallet, double amount)
            throws InvalidAmountException, InsufficientBalanceException, WalletLimitExceededException;

    double getBalance();

    void displayBalance();
}
