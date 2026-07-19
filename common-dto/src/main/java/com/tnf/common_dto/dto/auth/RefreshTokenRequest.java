package com.tnf.common_dto.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body carrying a refresh token (POST /api/auth/refresh and POST /api/auth/logout).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is required")
    private String refreshToken;
}
