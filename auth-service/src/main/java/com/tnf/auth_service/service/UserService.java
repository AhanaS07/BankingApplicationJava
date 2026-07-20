package com.tnf.auth_service.service;

import com.tnf.auth_service.entity.User;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

/**
 * User lifecycle operations: registration and lookup.
 */
public interface UserService {

    /**
     * Creates a new user from a registration request, hashing the password and assigning roles.
     *
     * @throws com.tnf.auth_service.exception.UserAlreadyExistsException if username/email is taken
     * @throws com.tnf.auth_service.exception.RoleNotFoundException      if a requested role is unknown
     */
    User register(RegisterRequest request);

    /** Loads a user by username or throws {@link com.tnf.auth_service.exception.UserNotFoundException}. */
    User getByUsername(String username);

    /** Maps a {@link User} to the shared {@link UserResponse} DTO. */
    UserResponse toResponse(User user);
}
