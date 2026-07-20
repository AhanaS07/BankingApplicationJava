package com.tnf.auth_service.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A grantable authority (e.g. {@code ROLE_USER}, {@code ROLE_ADMIN}).
 *
 * <p>Roles are seeded on startup by {@link com.tnf.auth_service.config.RoleInitializer} and referenced
 * by name from {@link User#getRoles()}.
 */
@Document(collection = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    private String id;

    /** Authority name, e.g. {@code ROLE_USER}. Unique across the collection. */
    @Indexed(unique = true)
    private String name;

    private String description;

    @CreatedDate
    private Instant createdDate;

    @LastModifiedDate
    private Instant lastModifiedDate;
}
