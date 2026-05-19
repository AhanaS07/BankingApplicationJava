package model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Transaction {

    private String transactionId;
    private double amount;
    private String type;        // DEPOSIT, WITHDRAW, TRANSFER
    private String timestamp;

    public Transaction(String transactionId, double amount, String type) {
        this.transactionId = transactionId;
        this.amount        = amount;
        this.type          = type;
        this.timestamp     = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public String getTransactionId() { return transactionId; }
    public double getAmount()        { return amount; }
    public String getType()          { return type; }
    public String getTimestamp()     { return timestamp; }

    @Override
    public String toString() {
        return "Transaction [ID=" + transactionId +
                ", Type=" + type +
                ", Amount=₹" + amount +
                ", Time=" + timestamp + "]";
    }
}
