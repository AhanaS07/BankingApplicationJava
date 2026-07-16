package com.tnf.common_dto.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for creating a wallet.
// Maps to the monolith's BankingService.createPaytmWallet/createPhonePeWallet(customer, openingBalance)
// (Main menu option 9 "Create wallet").
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {

    // The owning customer; customer-service owns the Customer itself.
    @NotBlank(message = "customerId is required")
    private String customerId;

    // Chooses the subclass the monolith instantiated (PaytmWallet vs PhonePeWallet).
    @NotBlank(message = "walletType is required")
    @Pattern(regexp = "PAYTM|PHONEPE", message = "walletType must be PAYTM or PHONEPE")
    private String walletType;

    // The monolith's openingBalance argument.
    @PositiveOrZero(message = "openingBalance cannot be negative")
    private BigDecimal openingBalance;
}
