package com.tnf.auth_service.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tnf.auth_service.config.JwtProperties;
import com.tnf.auth_service.entity.User;
import com.tnf.auth_service.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * HMAC-SHA256 implementation of {@link JwtService} backed by io.jsonwebtoken (jjwt).
 */
@Service
public class JwtServiceImpl implements JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtServiceImpl.class);
    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "uid";
    private static final String CUSTOMER_ID_CLAIM = "cid";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtServiceImpl(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getAccessTokenExpirationMs());
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(properties.getIssuer())
                .claim(USER_ID_CLAIM, user.getId())
                .claim(CUSTOMER_ID_CLAIM, user.getCustomerId())
                .claim(ROLES_CLAIM, user.getRoles())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected invalid JWT: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(String token) {
        Object raw = parseClaims(token).get(ROLES_CLAIM);
        if (raw instanceof Collection<?> collection) {
            Set<String> roles = new HashSet<>();
            collection.forEach(role -> roles.add(String.valueOf(role)));
            return roles;
        }
        return Set.of();
    }

    @Override
    public long getAccessTokenExpirationMs() {
        return properties.getAccessTokenExpirationMs();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
