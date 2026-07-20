package com.tnf.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** HMAC signing secret. Must be at least 32 bytes (256 bits) for HS256. */
    @NotBlank
    private String secret;

    /** Token issuer claim, echoed into every access token. */
    @NotBlank
    private String issuer = "auth-service";

    /** Access-token lifetime in milliseconds. */
    @Positive
    private long accessTokenExpirationMs = 900_000L; // 15 minutes

    /** Refresh-token lifetime in milliseconds. */
    @Positive
    private long refreshTokenExpirationMs = 604_800_000L; // 7 days
}
