package com.tnf.common_dto.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for topping up a wallet.
// Maps to the monolith's WalletOperations.addMoney(amount) / BankingService.walletAddMoney(walletId, amount)
// (Main menu option 10). The walletId comes from the URL path, not this body.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMoneyRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
