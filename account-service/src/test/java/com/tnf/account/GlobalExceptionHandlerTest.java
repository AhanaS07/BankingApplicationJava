package com.tnf.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.tnf.account.exception.AccountNotFoundException;
import com.tnf.account.exception.AccountTransferException;
import com.tnf.account.exception.CustomerNotFoundException;
import com.tnf.account.exception.CustomerServiceUnavailableException;
import com.tnf.account.exception.GlobalExceptionHandler;
import com.tnf.account.exception.InsufficientBalanceException;
import com.tnf.account.exception.InvalidAccountOperationException;
import com.tnf.account.exception.UnauthorizedAccountAccessException;
import com.tnf.common_dto.dto.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Unit tests for GlobalExceptionHandler — verifies that each exception type is mapped to the
 * correct HTTP status code and that the response body contains the exception message.
 * No Spring context is needed: the handler is a plain class and HttpServletRequest is mocked.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/accounts/test");
    }

    @Test
    void handleNotFound_returns404() {
        AccountNotFoundException ex = new AccountNotFoundException("Account not found: ACC1");

        ResponseEntity<ErrorResponse> resp = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getStatusCode().value());
        assertEquals("Account not found: ACC1", resp.getBody().getMessage());
    }

    @Test
    void handleCustomerNotFound_returns404() {
        // Also exercises CustomerNotFoundException single-arg constructor (previously uncovered)
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer cust-1 does not exist");

        ResponseEntity<ErrorResponse> resp = handler.handleCustomerNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND.value(), resp.getStatusCode().value());
        assertEquals("Customer cust-1 does not exist", resp.getBody().getMessage());
    }

    @Test
    void handleForbidden_returns403() {
        UnauthorizedAccountAccessException ex =
                new UnauthorizedAccountAccessException("You may only access your own account(s)");

        ResponseEntity<ErrorResponse> resp = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN.value(), resp.getStatusCode().value());
        assertEquals("You may only access your own account(s)", resp.getBody().getMessage());
    }

    @Test
    void handleCustomerServiceDown_returns503() {
        CustomerServiceUnavailableException ex =
                new CustomerServiceUnavailableException("customer-service is unavailable", new RuntimeException());

        ResponseEntity<ErrorResponse> resp = handler.handleCustomerServiceDown(ex, request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), resp.getStatusCode().value());
        assertEquals("customer-service is unavailable", resp.getBody().getMessage());
    }

    @Test
    void handleInsufficientBalance_returns422() {
        InsufficientBalanceException ex =
                new InsufficientBalanceException("Balance would fall below the minimum");

        ResponseEntity<ErrorResponse> resp = handler.handleInsufficientBalance(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), resp.getStatusCode().value());
        assertEquals("Balance would fall below the minimum", resp.getBody().getMessage());
    }

    @Test
    void handleInvalidOperation_returns400() {
        InvalidAccountOperationException ex =
                new InvalidAccountOperationException("Cannot transfer to the same account");

        ResponseEntity<ErrorResponse> resp = handler.handleInvalidOperation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value());
        assertEquals("Cannot transfer to the same account", resp.getBody().getMessage());
    }

    @Test
    void handleTransferFailure_returns409_whenDebitWasRolledBack() {
        // reconciled=true means no money moved → 409 Conflict (safe to retry)
        AccountTransferException ex = new AccountTransferException(
                "Transfer failed; debit was rolled back", true, new RuntimeException());

        ResponseEntity<ErrorResponse> resp = handler.handleTransferFailure(ex, request);

        assertEquals(HttpStatus.CONFLICT.value(), resp.getStatusCode().value());
        assertEquals("Transfer failed; debit was rolled back", resp.getBody().getMessage());
    }

    @Test
    void handleTransferFailure_returns500_whenRollbackAlsoFailed() {
        // reconciled=false means balances may be inconsistent → 500 (manual intervention needed)
        AccountTransferException ex = new AccountTransferException(
                "Transfer failed and debit could not be rolled back", false, new RuntimeException());

        ResponseEntity<ErrorResponse> resp = handler.handleTransferFailure(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), resp.getStatusCode().value());
        assertEquals("Transfer failed and debit could not be rolled back", resp.getBody().getMessage());
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("No enum constant INVALID_TYPE");

        ResponseEntity<ErrorResponse> resp = handler.handleIllegalArgument(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value());
        assertEquals("No enum constant INVALID_TYPE", resp.getBody().getMessage());
    }

    @Test
    void handleValidation_returns400_withFieldErrorsSummary() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("createAccountRequest", "customerId", "must not be blank"),
                new FieldError("createAccountRequest", "accountType", "must not be blank")));

        ResponseEntity<ErrorResponse> resp = handler.handleValidation(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), resp.getStatusCode().value());
        assertEquals("customerId: must not be blank, accountType: must not be blank",
                resp.getBody().getMessage());
    }

    @Test
    void handleUnexpected_returns500() {
        Exception ex = new RuntimeException("Unexpected DB failure");

        ResponseEntity<ErrorResponse> resp = handler.handleUnexpected(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), resp.getStatusCode().value());
        assertEquals("An unexpected error occurred", resp.getBody().getMessage());
    }
}
