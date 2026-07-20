package com.tnf.auth_service.exception;

/**
 * Thrown when a requested role name cannot be resolved to a seeded {@link com.tnf.auth_service.entity.Role}.
 */
public class RoleNotFoundException extends AuthException {

    public RoleNotFoundException(String message) {
        super(message);
    }
}
