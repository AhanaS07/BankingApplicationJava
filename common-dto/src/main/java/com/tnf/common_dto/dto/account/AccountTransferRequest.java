package com.tnf.common_dto.dto.account;

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
 * <p>Named {@code AccountTransferRequest} to avoid clashing with the wallet
 * {@code com.tnf.common_dto.dto.wallet.TransferRequest} that also lives in the shared jar.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransferRequest {

    @NotBlank(message = "targetAccountNumber is required")
    private String targetAccountNumber;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
