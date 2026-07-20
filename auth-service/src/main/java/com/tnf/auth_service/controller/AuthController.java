package com.tnf.auth_service.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tnf.auth_service.security.CustomUserDetails;
import com.tnf.auth_service.service.AuthService;
import com.tnf.common_dto.dto.auth.JwtResponse;
import com.tnf.common_dto.dto.auth.LoginRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenResponse;
import com.tnf.common_dto.dto.auth.RegisterRequest;
import com.tnf.common_dto.dto.auth.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST endpoints for authentication. All business logic is delegated to {@link AuthService}.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login and JWT lifecycle operations")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a user and returns an initial JWT token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - username '{}'", request.getUsername());
        JwtResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate", description = "Validates credentials and returns a JWT token pair")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - username '{}'", request.getUsername());
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh tokens", description = "Rotates the refresh token and issues a new access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New token pair issued"),
            @ApiResponse(responseCode = "401", description = "Refresh token expired or revoked"),
            @ApiResponse(responseCode = "404", description = "Refresh token not found")
    })
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/refresh - rotating refresh token");
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revokes the supplied refresh token")
    @ApiResponse(responseCode = "204", description = "Logged out")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("POST /api/auth/logout - revoking refresh token");
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/profile")
    @Operation(summary = "Current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<UserResponse> profile(@AuthenticationPrincipal CustomUserDetails principal) {
        log.info("GET /api/auth/profile - user '{}'", principal.getUsername());
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUsername()));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate access token",
            description = "Returns the resolved principal when the bearer token is valid; used by other services",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
    })
    public ResponseEntity<UserResponse> validate(@AuthenticationPrincipal CustomUserDetails principal) {
        log.debug("GET /api/auth/validate - token valid for user '{}'", principal.getUsername());
        return ResponseEntity.ok(authService.getCurrentUser(principal.getUsername()));
    }
}
