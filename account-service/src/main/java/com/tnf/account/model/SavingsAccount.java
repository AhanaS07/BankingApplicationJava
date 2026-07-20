package com.tnf.account.model;

import java.math.BigDecimal;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Savings account: the balance may never drop below {@link #minimumBalance} after a
 * withdrawal, and it accrues interest at {@link #interestRate}.
 *
 * <p>Annotated with the same collection as the base so all account subtypes live in one
 * "accounts" collection.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Document(collection = "accounts")
public class SavingsAccount extends BankAccount {

    /** Balance must stay at or above this value after any withdrawal. */
    private BigDecimal minimumBalance;

    /** Annual interest rate as a fraction (e.g. 0.035 = 3.5%). */
    private BigDecimal interestRate;
}
