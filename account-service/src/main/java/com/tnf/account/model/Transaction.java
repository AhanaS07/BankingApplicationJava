package com.tnf.account.model;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Immutable record of a single movement of funds against an account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "transactions")
public class Transaction {

    @Id
    private String id;

    /** The account this row belongs to (the {@code BankAccount.id}). */
    private String accountId;

    /** The counterparty account id; set only for TRANSFER rows, null otherwise. */
    private String targetAccountId;

    private BigDecimal amount;

    private TransactionType transactionType;

    private Instant timestamp;
}
