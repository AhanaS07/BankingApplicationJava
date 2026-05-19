package wallet;

import exception.InsufficientBalanceException;
import exception.WalletLimitExceededException;

public interface WalletOperations {
    void addMoney(double amount) throws WalletLimitExceededException, exception.InvalidAmountException;

    void payBill(String billName, double amount) throws InsufficientBalanceException, exception.InvalidAmountException;

    void transferToWallet(WalletOperations target, double amount) throws InsufficientBalanceException, WalletLimitExceededException, exception.InvalidAmountException;

    double getBalance();
    
    void displayDetails();
}