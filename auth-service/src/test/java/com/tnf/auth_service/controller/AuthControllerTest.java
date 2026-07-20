package com.tnf.auth_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tnf.auth_service.security.CustomUserDetailsService;
import com.tnf.auth_service.service.AuthService;
import com.tnf.auth_service.service.JwtService;
import com.tnf.common_dto.dto.auth.JwtResponse;
import com.tnf.common_dto.dto.auth.LoginRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenRequest;
import com.tnf.common_dto.dto.auth.RefreshTokenResponse;
import com.tnf.common_dto.dto.auth.RegisterRequest;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    // Required to satisfy the JwtAuthenticationFilter bean pulled into the web slice.
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void registerReturns201WithTokens() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("alice").email("alice@example.com").password("Str0ng@Pass").build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(JwtResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer")
                .username("alice").roles(Set.of("ROLE_USER")).build());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .username("alice").email("alice@example.com").password("weak").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void loginReturns200() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(JwtResponse.builder()
                .accessToken("access").refreshToken("refresh").tokenType("Bearer").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("alice", "Str0ng@Pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void refreshReturns200() throws Exception {
        when(authService.refresh(any(RefreshTokenRequest.class))).thenReturn(RefreshTokenResponse.builder()
                .accessToken("access2").refreshToken("new").tokenType("Bearer").build());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("old"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new"));
    }

    @Test
    void logoutReturns204() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("some-token"))))
                .andExpect(status().isNoContent());

        verify(authService).logout(any(RefreshTokenRequest.class));
    }
}
