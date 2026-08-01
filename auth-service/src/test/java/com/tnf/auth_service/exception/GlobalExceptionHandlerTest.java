package com.tnf.auth_service.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.tnf.common_dto.dto.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
    }

    @Test
    void handleUserExistsReturns409() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUserExists(new UserAlreadyExistsException("taken"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("taken");
        assertThat(response.getBody().getPath()).isEqualTo("/api/auth/login");
    }

    @Test
    void handleNotFoundReturns404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new UserNotFoundException("missing"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).isEqualTo("missing");
    }

    @Test
    void handleUnauthorizedReturns401() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorized(new BadCredentialsException("bad creds"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    void handleValidationJoinsFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("obj", "username", "must not be blank"),
                new FieldError("obj", "password", "too weak")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage())
                .contains("username: must not be blank")
                .contains("password: too weak");
    }

    @Test
    void handleCustomerProvisioningReturns503() {
        ResponseEntity<ErrorResponse> response = handler.handleCustomerProvisioning(
                new CustomerProvisioningException("downstream down", new RuntimeException()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().getMessage()).isEqualTo("downstream down");
    }

    @Test
    void handleAuthReturns400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleAuth(new AuthException("bad request"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).isEqualTo("bad request");
    }

    @Test
    void handleUnexpectedReturns500WithGenericMessage() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
