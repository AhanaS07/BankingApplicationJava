package com.tnf.common_dto.dto.auth;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response returned after a successful register/login: the issued token pair plus a user summary.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    private String accessToken;

    private String refreshToken;

    // Always "Bearer".
    private String tokenType;

    private String username;

    // Id of the linked customer profile (customer-service), also carried as a JWT claim.
    private String customerId;

    private Set<String> roles;
}
