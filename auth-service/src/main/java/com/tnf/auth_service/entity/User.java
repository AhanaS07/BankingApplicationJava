package com.tnf.auth_service.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A registered principal that can authenticate against the service.
 *
 * <p>The password is always stored as a BCrypt hash — the raw password never touches the database.
 * {@code username} and {@code email} are uniquely indexed.
 */
@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    /** BCrypt hash of the user's password. */
    @Field("password")
    private String password;

    /** Authority names granted to this user (e.g. {@code ROLE_USER}). */
    @Builder.Default
    private Set<String> roles = new HashSet<>();

    /** Whether the account is active; a disabled account cannot authenticate. */
    @Builder.Default
    private boolean enabled = true;

    @CreatedDate
    private Instant createdDate;

    @LastModifiedDate
    private Instant lastModifiedDate;
}
