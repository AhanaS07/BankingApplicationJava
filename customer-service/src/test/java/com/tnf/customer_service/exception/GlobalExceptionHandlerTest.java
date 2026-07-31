package com.tnf.customer_service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.tnf.common_dto.dto.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private HttpServletRequest requestFor(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(uri);
        return request;
    }

    @Test
    void handleNotFound_returns404WithMessageAndPath() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new CustomerNotFoundException("Customer not found with id: 1"), requestFor("/api/customers/1"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Customer not found with id: 1", response.getBody().getMessage());
        assertEquals("/api/customers/1", response.getBody().getPath());
    }

    @Test
    void handleDuplicate_returns409WithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicate(
                new DuplicateCustomerException("Customer already exists with email: a@b.com"),
                requestFor("/api/customers"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Customer already exists with email: a@b.com", response.getBody().getMessage());
    }

    @Test
    void handleForbidden_returns403WithMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleForbidden(
                new UnauthorizedCustomerAccessException("You may only access your own customer profile"),
                requestFor("/api/customers/2"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You may only access your own customer profile", response.getBody().getMessage());
    }

    @Test
    void handleValidation_returns400WithFieldErrorsJoined() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors())
                .thenReturn(List.of(new FieldError("customerDto", "email", "must not be blank")));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, requestFor("/api/customers"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("email: must not be blank", response.getBody().getMessage());
    }

    // Unmapped exceptions must never leak internal details (stack traces, connection strings, etc.)
    // to the client -> always a generic message, regardless of what the underlying exception says.
    @Test
    void handleUnexpected_returns500WithGenericMessage_notLeakingExceptionDetails() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("connection refused: mongodb://internal-host:27017"),
                requestFor("/api/customers"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected error occurred", response.getBody().getMessage());
    }
}
