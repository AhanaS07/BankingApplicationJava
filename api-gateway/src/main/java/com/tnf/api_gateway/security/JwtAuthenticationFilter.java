package com.tnf.api_gateway.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

/**
 * Edge authentication for the whole system.
 *
 * <p>Runs on every request the gateway routes. Public endpoints (auth login/register, swagger,
 * actuator) pass through untouched; every other request must carry a valid {@code Bearer} JWT issued
 * by auth-service. On success the validated claims are forwarded downstream as {@code X-Auth-*}
 * headers so customer/wallet/account can identify the caller without re-validating the token.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Identity/trust headers the gateway alone is allowed to set. Any copy supplied by an inbound
     * client is stripped before routing so it can never be spoofed or smuggled to a downstream
     * service ({@code X-Internal-Api-Key} gates customer provisioning; {@code X-Auth-*} carry the
     * authenticated identity that services trust).
     */
    private static final List<String> TRUSTED_HEADERS = List.of(
            "X-Internal-Api-Key",
            "X-Auth-Username",
            "X-Auth-User-Id",
            "X-Auth-Customer-Id",
            "X-Auth-Roles");

    /** Endpoints reachable without a token. */
    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/v3/api-docs/**",
            "/api/*/v3/api-docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/**",
            "/fallback/**");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey signingKey;
    private final String issuer;

    public JwtAuthenticationFilter(@Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer:auth-service}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublic(path)) {
            // Even unauthenticated requests must not carry client-supplied trust headers downstream.
            ServerHttpRequest cleaned = exchange.getRequest().mutate()
                    .headers(this::stripTrustedHeaders)
                    .build();
            return chain.filter(exchange.mutate().request(cleaned).build());
        }

        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            // Logged at WARN to match the invalid/expired-token rejection below, so every 401 the
            // gateway emits is recorded. The path only — never the Authorization header value.
            log.warn("Rejected request to {}: missing or malformed Authorization header", path);
            return unauthorized(exchange, "Missing or malformed Authorization header");
        }

        String token = header.substring(BEARER_PREFIX.length());
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    // Drop any inbound copies first so the values below are the only ones downstream sees.
                    .headers(this::stripTrustedHeaders)
                    .header("X-Auth-Username", nullSafe(claims.getSubject()))
                    .header("X-Auth-User-Id", nullSafe(claims.get("uid")))
                    .header("X-Auth-Customer-Id", nullSafe(claims.get("cid")))
                    .header("X-Auth-Roles", nullSafe(claims.get("roles")))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Rejected request to {}: {}", path, ex.getMessage());
            return unauthorized(exchange, "Invalid or expired token");
        }
    }

    private void stripTrustedHeaders(HttpHeaders headers) {
        TRUSTED_HEADERS.forEach(headers::remove);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private static String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Override
    public int getOrder() {
        // Run before the routing filter so unauthenticated requests never reach downstream services.
        return -1;
    }
}
