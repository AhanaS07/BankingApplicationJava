package com.tnf.common_dto.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response returned after exchanging a refresh token for a new access token (POST /api/auth/refresh).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {

    private String accessToken;

    private String refreshToken;

    // Always "Bearer".
    private String tokenType;
}
