package com.tnf.auth_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tnf.auth_service.entity.RefreshToken;

/**
 * Data-access for {@link RefreshToken} documents.
 */
@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserId(String userId);

    void deleteByToken(String token);
}
