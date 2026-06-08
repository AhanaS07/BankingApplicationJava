package wallet;

import model.Customer;

public class PhonePeWallet extends AbstractWallet {

    public PhonePeWallet(String walletId, Customer linkedCustomer, double openingBalance) {
        super(walletId, linkedCustomer, openingBalance);
    }

    @Override
    public String toString() {
        return "PhonePeWallet{id=" + getWalletId() + ", balance=" + getBalance() + "}";
    }
}
