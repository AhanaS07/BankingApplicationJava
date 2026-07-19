package com.tnf.common_dto.dto.auth;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Response describing the authenticated user's profile (GET /api/auth/profile). Never carries the password.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String id;

    private String username;

    private String email;

    private Set<String> roles;

    private boolean enabled;
}
