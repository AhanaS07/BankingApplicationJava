package com.tnf.common_dto.dto.auth;

import java.util.Set;

import com.tnf.common_dto.dto.customer.AddressDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Request body for registering a new user (POST /api/auth/register).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "username is required")
    @Size(min = 3, max = 50, message = "username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
            message = "username may only contain letters, digits, dot, underscore or hyphen")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    // Raw password; auth-service hashes it with BCrypt before persistence.
    // Policy: min 8 chars with at least one lowercase, one uppercase, one digit and one special char.
    @NotBlank(message = "password is required")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
            message = "password must be at least 8 characters and include an uppercase letter, "
                    + "a lowercase letter, a number and a special character")
    private String password;

    // Optional; auth-service defaults to ROLE_USER when absent or empty.
    private Set<String> roles;

    // --- Customer profile fields ---
    // On registration, auth-service creates the matching Customer in customer-service using these.

    @NotBlank(message = "firstName is required")
    private String firstName;

    @NotBlank(message = "lastName is required")
    private String lastName;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "phone must be exactly 10 digits")
    private String phone;

    // Optional postal address for the customer profile.
    private AddressDto address;
}
