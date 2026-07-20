package com.tnf.auth_service.service;

import com.tnf.common_dto.dto.auth.JwtResponse;
import com.tnf.common_dto.dto.auth.LoginRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenResponse;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

/**
 * Orchestrates the authentication use-cases: registration, login, token refresh, logout and profile.
 */
public interface AuthService {

    /** Registers a new user and returns an initial access/refresh token pair. */
    JwtResponse register(RegisterRequest request);

    /** Authenticates credentials and returns a fresh access/refresh token pair. */
    JwtResponse login(LoginRequest request);

    /** Rotates a refresh token: revokes the presented one and issues a new token pair. */
    RefreshTokenResponse refresh(RefreshTokenRequest request);

    /** Revokes the presented refresh token, ending that session. */
    void logout(RefreshTokenRequest request);

    /** Returns the profile of the currently authenticated user. */
    UserResponse getCurrentUser(String username);
}
