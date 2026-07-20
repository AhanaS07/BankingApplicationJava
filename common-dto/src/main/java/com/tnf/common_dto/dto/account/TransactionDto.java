package com.tnf.common_dto.dto.account;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Cross-service view of a single account transaction record.
// transactionType is a String (not an enum) on purpose: the TransactionType enum lives in
// account-service's model package.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {

    private String id;

    @NotBlank(message = "accountId is required")
    private String accountId;

    // The counterparty account id; set only for TRANSFER records, null otherwise.
    private String targetAccountId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    // "DEPOSIT", "WITHDRAWAL" or "TRANSFER".
    @NotBlank(message = "transactionType is required")
    @Pattern(regexp = "DEPOSIT|WITHDRAWAL|TRANSFER",
            message = "transactionType must be DEPOSIT, WITHDRAWAL or TRANSFER")
    private String transactionType;

    private Instant timestamp;
}
