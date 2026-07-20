package com.tnf.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for opening an account. Account-service local (not shared): it is an inbound
 * API contract, whereas {@code common-dto}'s {@code BankAccountDto} is the cross-service view.
 * The type-specific fields are optional; sensible defaults are applied when omitted.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequest {

    @NotBlank(message = "customerId is required")
    private String customerId;

    @NotBlank(message = "accountType is required")
    @Pattern(regexp = "SAVINGS|CURRENT", message = "accountType must be SAVINGS or CURRENT")
    private String accountType;

    @PositiveOrZero(message = "initialDeposit cannot be negative")
    private BigDecimal initialDeposit;

    /** SAVINGS only. */
    @PositiveOrZero(message = "minimumBalance cannot be negative")
    private BigDecimal minimumBalance;

    /** SAVINGS only. */
    @PositiveOrZero(message = "interestRate cannot be negative")
    private BigDecimal interestRate;

    /** CURRENT only. */
    @PositiveOrZero(message = "overdraftLimit cannot be negative")
    private BigDecimal overdraftLimit;
}
