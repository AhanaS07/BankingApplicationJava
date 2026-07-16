package com.tnf.common_dto.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for a wallet-to-wallet transfer.
// Maps to the monolith's WalletOperations.transferToWallet(target, amount) /
// BankingService.walletTransfer(fromWalletId, toWalletId, amount) (Main menu option 12).
// The source walletId comes from the URL path; this body carries the target and amount.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {

    // The monolith's "target" wallet argument.
    @NotBlank(message = "targetWalletId is required")
    private String targetWalletId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
