package com.tnf.auth_service.exception;

/**
 * Thrown when registration cannot create the linked customer profile because customer-service is
 * unreachable or rejected the request. Registration is aborted so no orphaned auth user is created.
 */
public class CustomerProvisioningException extends AuthException {

    public CustomerProvisioningException(String message, Throwable cause) {
        super(message, cause);
    }
}
