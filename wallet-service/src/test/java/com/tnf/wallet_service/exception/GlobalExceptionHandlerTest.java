package com.tnf.wallet_service.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.tnf.common_dto.dto.common.ErrorResponse;

/*
Each handler is exercised for the status code it maps to.
    -> 404 for missing things
    -> 403 for ownership violations
    -> 422 for business-rule rejections
    -> 503 for a fail-closed dependency
    -> 409 vs 500 for the two flavours of failed transfer. A client may safely retry a 409 but must not retry a 500.
 */

class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/wallets/W1";

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI(PATH);
    }

    private static void assertBody(ResponseEntity<ErrorResponse> response, HttpStatus expectedStatus,
            String expectedMessage) {
        assertEquals(expectedStatus, response.getStatusCode());
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(expectedStatus.value(), body.getStatus());
        assertEquals(expectedStatus.getReasonPhrase(), body.getError());
        assertEquals(expectedMessage, body.getMessage());
        assertEquals(PATH, body.getPath());
        assertNotNull(body.getTimestamp());
    }

    @Test
    @DisplayName("a missing wallet is a 404")
    void walletNotFoundMapsTo404() {
        assertBody(handler.handleNotFound(new WalletNotFoundException("Wallet not found with id: W1"), request),
                HttpStatus.NOT_FOUND, "Wallet not found with id: W1");
    }

    @Test
    @DisplayName("a missing owning customer is a 404")
    void customerNotFoundMapsTo404() {
        assertBody(handler.handleCustomerNotFound(new CustomerNotFoundException("Customer c1 does not exist"), request),
                HttpStatus.NOT_FOUND, "Customer c1 does not exist");
    }

    @Test
    @DisplayName("an ownership violation is a 403, not a 404")
    void unauthorizedAccessMapsTo403() {
        assertBody(handler.handleForbidden(
                new UnauthorizedWalletAccessException("You may only access your own wallet(s)"), request),
                HttpStatus.FORBIDDEN, "You may only access your own wallet(s)");
    }

    @Test
    @DisplayName("an unreachable customer-service is a 503, so the caller knows to retry")
    void customerServiceDownMapsTo503() {
        CustomerServiceUnavailableException ex =
                new CustomerServiceUnavailableException("customer-service is unavailable", new RuntimeException());

        assertBody(handler.handleCustomerServiceDown(ex, request),
                HttpStatus.SERVICE_UNAVAILABLE, "customer-service is unavailable");
    }

    @Test
    @DisplayName("business-rule rejections share a single 422 handler")
    void businessRuleViolationsMapTo422() {
        assertBody(handler.handleBusinessRule(new InvalidAmountException("Invalid opening balance: -1"), request),
                HttpStatus.UNPROCESSABLE_ENTITY, "Invalid opening balance: -1");
        assertBody(handler.handleBusinessRule(new InsufficientBalanceException("insufficient balance"), request),
                HttpStatus.UNPROCESSABLE_ENTITY, "insufficient balance");
        assertBody(handler.handleBusinessRule(new WalletLimitExceededException("limit exceeded"), request),
                HttpStatus.UNPROCESSABLE_ENTITY, "limit exceeded");
    }

    @Test
    @DisplayName("a reconciled transfer failure is a 409 — no money moved, safe to retry")
    void reconciledTransferFailureMapsTo409() {
        WalletTransferException ex =
                new WalletTransferException("rolled back", true, new RuntimeException("credit failed"));

        assertTrue(ex.isReconciled());
        assertBody(handler.handleTransferFailure(ex, request), HttpStatus.CONFLICT, "rolled back");
    }

    @Test
    @DisplayName("an unreconciled transfer failure is a 500 — balances are inconsistent, do not retry")
    void unreconciledTransferFailureMapsTo500() {
        WalletTransferException ex =
                new WalletTransferException("manual reconciliation required", false, new RuntimeException("boom"));

        assertFalse(ex.isReconciled());
        assertBody(handler.handleTransferFailure(ex, request),
                HttpStatus.INTERNAL_SERVER_ERROR, "manual reconciliation required");
    }

    @Test
    @DisplayName("an unknown walletType (WalletType.valueOf) is a 400")
    void illegalArgumentMapsTo400() {
        assertBody(handler.handleIllegalArgument(
                new IllegalArgumentException("No enum constant WalletType.GPAY"), request),
                HttpStatus.BAD_REQUEST, "No enum constant WalletType.GPAY");
    }

    @Test
    @DisplayName("bean-validation field errors are flattened into one 400 message")
    void validationFailuresAreFlattenedInto400() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "amount", "amount must be positive"));
        binding.addError(new FieldError("request", "targetWalletId", "targetWalletId is required"));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(
                new MethodArgumentNotValidException(annotatedParameter(), binding), request);

        assertBody(response, HttpStatus.BAD_REQUEST,
                "amount: amount must be positive, targetWalletId: targetWalletId is required");
    }

    @Test
    @DisplayName("anything unmapped is a generic 500 that does not leak internals")
    void unexpectedErrorsAreMaskedAs500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnexpected(new IllegalStateException("mongodb://user:secret@host down"), request);

        // The original message must not reach the client.
        assertBody(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Test
    @DisplayName("the error body carries a timestamp at handling time")
    void bodyCarriesAHandlingTimestamp() {
        LocalDateTime before = LocalDateTime.now();

        ErrorResponse body = handler.handleNotFound(new WalletNotFoundException("gone"), request).getBody();

        assertNotNull(body);
        assertFalse(body.getTimestamp().isBefore(before));
    }

    // MethodArgumentNotValidException needs a MethodParameter
    private static MethodParameter annotatedParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validatedTarget", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void validatedTarget(String body) {
        // Reflection target only — never invoked.
    }
}
