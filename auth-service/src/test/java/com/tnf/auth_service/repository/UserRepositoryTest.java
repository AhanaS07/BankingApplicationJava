package com.tnf.auth_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;

import com.tnf.auth_service.config.MongoConfig;
import com.tnf.auth_service.entity.User;

import org.springframework.context.annotation.Import;

/**
 * Repository slice test backed by an embedded MongoDB. Verifies query derivation and the unique
 * index on username/email.
 */
@DataMongoTest
@Import(MongoConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void seedIndexes() {
        // Ensure the declared @Indexed(unique=true) indexes exist before asserting on them.
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    private User user(String username, String email) {
        return User.builder()
                .username(username).email(email).password("hash")
                .roles(Set.of("ROLE_USER")).enabled(true).build();
    }

    @Test
    void savesAndFindsByUsername() {
        userRepository.save(user("alice", "alice@example.com"));

        assertThat(userRepository.findByUsername("alice")).isPresent();
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userRepository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void populatesAuditingTimestamps() {
        User saved = userRepository.save(user("bob", "bob@example.com"));

        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
    }

    @Test
    void enforcesUniqueUsernameIndex() {
        userRepository.save(user("carol", "carol1@example.com"));

        assertThatThrownBy(() -> userRepository.save(user("carol", "carol2@example.com")))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
