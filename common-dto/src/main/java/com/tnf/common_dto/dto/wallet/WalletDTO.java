package com.tnf.common_dto.dto.wallet;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// No wallet business rules (MAX_BALANCE, DAILY_LIMIT, daily rollover) live here.
// walletType is a String (not an enum) on purpose: the enum lives in wallet-service's model package.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletDTO {

    // Null/absent on a create request, populated on responses.
    private String walletId;

    // Reference to the owning customer (customer-service owns the Customer itself).
    @NotBlank(message = "customerId is required")
    private String customerId;

    // "PAYTM" or "PHONEPE" — validated against wallet-service's WalletType enum.
    @NotBlank(message = "walletType is required")
    @Pattern(regexp = "PAYTM|PHONEPE", message = "walletType must be PAYTM or PHONEPE")
    private String walletType;

    // Current wallet balance. Never negative.
    @PositiveOrZero(message = "Balance cannot be negative")
    private BigDecimal balance;
}
