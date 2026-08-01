package com.tnf.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class RestAuthenticationEntryPointTest {

    // findAndRegisterModules() picks up JavaTimeModule so LocalDateTime serialises,
    // matching the Spring Boot-configured ObjectMapper the real bean receives.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);

    @Test
    void commenceWrites401JsonBody() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AuthenticationException authException = mock(AuthenticationException.class);
        when(request.getRequestURI()).thenReturn("/api/auth/profile");
        when(authException.getMessage()).thenReturn("Full authentication is required");

        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(captured));

        entryPoint.commence(request, response, authException);

        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(response).setContentType(MediaType.APPLICATION_JSON_VALUE);

        JsonNode body = objectMapper.readTree(captured.toByteArray());
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("error").asText()).isEqualTo(HttpStatus.UNAUTHORIZED.getReasonPhrase());
        assertThat(body.get("message").asText()).isEqualTo("Authentication is required to access this resource");
        assertThat(body.get("path").asText()).isEqualTo("/api/auth/profile");
    }

    /** Minimal ServletOutputStream that funnels bytes into a buffer for assertions. */
    private static final class DelegatingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream target;

        DelegatingServletOutputStream(ByteArrayOutputStream target) {
            this.target = target;
        }

        @Override
        public void write(int b) {
            target.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // no-op
        }
    }
}
