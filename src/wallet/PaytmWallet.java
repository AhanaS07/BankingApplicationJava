package wallet;

import model.Customer;

public class PaytmWallet extends AbstractWallet {

    public PaytmWallet(String walletId, Customer linkedCustomer, double openingBalance) {
        super(walletId, linkedCustomer, openingBalance);
    }

    @Override
    public String toString() {
        return "PaytmWallet{id=" + getWalletId() + ", balance=" + getBalance() + "}";
    }
}
