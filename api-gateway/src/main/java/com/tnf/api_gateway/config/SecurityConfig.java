package com.tnf.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security for the gateway.
 *
 * <p>Spring Security is on the classpath (WebFlux), which by default would lock every route behind
 * HTTP Basic. We disable that here and let {@link com.tnf.api_gateway.security.JwtAuthenticationFilter}
 * perform JWT enforcement at the edge instead. CSRF is disabled because the gateway is a stateless,
 * token-based API entry point (no browser sessions/cookies).
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                // Authorization is enforced by the JWT GlobalFilter; permit at the security layer.
                .authorizeExchange(exchange -> exchange.anyExchange().permitAll())
                .build();
    }
}
