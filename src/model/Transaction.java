package model;
import exception.InvalidAmountException;
import java.time.Instant;
public class Transaction
{
private static int idCounter = 0;
private int transactionId ;
private double amount ;
private String type ;
private Instant timestamp;
public Transaction(){
    this.transactionId = ++idCounter;
}
public Transaction(double amount, String type) {
    this.transactionId = ++idCounter;
    setAmount(amount);
    setType(type);
    this.timestamp = Instant.now();
}
public int getTransactionId() {
    return transactionId;
}
public void setTransactionId(int transactionId) {
    this.transactionId = transactionId;
}
public double getAmount() {
    return amount;
}
public void setAmount(double amount) {
    if(amount>0)
    {
    this.amount = amount;
    }
    else
    {
        throw new InvalidAmountException("Please enter a valid amount: "+amount);
    }
}
public String getType() {
    return type;
}
public void setType(String type) {
    if (type == null || type.isBlank()) {
        throw new IllegalArgumentException("Transaction type cannot be null or empty");
    }
    this.type = type;
}
public Instant getTimestamp() {
    return timestamp;
}
public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
}




}

