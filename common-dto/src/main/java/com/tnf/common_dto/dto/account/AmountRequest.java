package com.tnf.common_dto.dto.account;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body carrying a single positive amount, used for deposit and withdrawal.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmountRequest {

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;
}
