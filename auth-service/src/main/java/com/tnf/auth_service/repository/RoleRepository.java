package com.tnf.auth_service.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.tnf.auth_service.entity.Role;

/**
 * Data-access for {@link Role} documents.
 */
@Repository
public interface RoleRepository extends MongoRepository<Role, String> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}
