package com.banking.model;

import com.banking.exception.InvalidAmountException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {

    private final String transactionId;
    private final double amount;
    private final TransactionType type;
    private final LocalDateTime timestamp;
    private String status;

    public Transaction(String transactionId, double amount, TransactionType type, String status) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero: " + amount);
        }
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.amount = amount;
        this.status = status == null ? "SUCCESS" : status;
        this.timestamp = LocalDateTime.now();
    }

    public String getTransactionId() { return transactionId; }
    public double getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Transaction{id=" + transactionId +
                ", type=" + type +
                ", amount=" + amount +
                ", status=" + status +
                ", at=" + timestamp + "}";
    }
}
