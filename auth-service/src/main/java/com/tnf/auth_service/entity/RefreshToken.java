package com.tnf.auth_service.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A persisted, opaque refresh token.
 *
 * <p>Storing refresh tokens (rather than making them stateless JWTs) is what enables revocation and
 * rotation: on every refresh the presented token is marked {@code revoked} and a new one is issued.
 * The {@code expiryDate} carries a TTL index so MongoDB purges expired documents automatically.
 */
@Document(collection = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    private String id;

    /** The opaque token value handed to the client. Unique and indexed for fast lookup. */
    @Indexed(unique = true)
    private String token;

    /** Id of the {@link User} this token was issued to. */
    @Indexed
    private String userId;

    /** Absolute expiry instant; also a TTL index so Mongo removes the document once it passes. */
    @Indexed(expireAfterSeconds = 0)
    private Instant expiryDate;

    /** Whether the token has been explicitly revoked (rotation or logout). */
    @Builder.Default
    private boolean revoked = false;

    @CreatedDate
    private Instant createdDate;
}
