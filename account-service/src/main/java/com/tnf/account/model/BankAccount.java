package com.tnf.account.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base account document. {@link SavingsAccount} and {@link CurrentAccount} share the single
 * "accounts" collection; Spring Data writes a "_class" discriminator so the correct subtype
 * is rehydrated on read. {@link #type} is stored explicitly as well so callers can filter/map
 * without depending on the discriminator.
 *
 * <p>Uses {@code @Getter/@Setter} rather than {@code @Data} on purpose: {@code @Data} generates
 * equals/hashCode that misbehave across an inheritance hierarchy.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Document(collection = "accounts")
public abstract class BankAccount {

    @Id
    private String id;

    private String accountNumber;

    private String customerId;

    private BigDecimal balance;

    private AccountType type;
}
