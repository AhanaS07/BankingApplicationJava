package com.tnf.account.model;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Current account: the balance may go negative down to {@code -overdraftLimit}.
 *
 * <p>Annotated with the same collection as the base so all account subtypes live in one
 * "accounts" collection.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Document(collection = "accounts")
public class CurrentAccount extends BankAccount {

    /** Maximum permitted overdraft; the effective balance floor is {@code -overdraftLimit}. */
    private BigDecimal overdraftLimit;
}
