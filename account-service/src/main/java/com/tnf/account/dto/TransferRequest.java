package com.tnf.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for an account-to-account transfer. The source account comes from the URL path;
 * this body carries the target account number and the amount.
 *
 * <p>Distinct from {@code common-dto}'s wallet {@code TransferRequest} — that one is a wallet
 * concern and lives in the shared jar; this is the account-service contract.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    @NotBlank(message = "targetAccountNumber is required")
    private String targetAccountNumber;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
