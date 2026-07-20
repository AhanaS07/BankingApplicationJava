package com.tnf.common_dto.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for paying a bill from a wallet.
// Maps to monolith's WalletOperations.payBill(amount) / BankingService.walletPayBill(walletId, amount)
// (Main menu option 11). walletId comes from the URL path, not this body.

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayBillRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
