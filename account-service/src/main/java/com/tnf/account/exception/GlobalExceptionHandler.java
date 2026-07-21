package com.tnf.account.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.tnf.common_dto.dto.common.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

// Maps account-service exceptions to meaningful HTTP responses, using the shared ErrorResponse contract.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException ex, HttpServletRequest request) {
        logger.warn("Account not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // The referenced owning customer does not exist in customer-service.
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest request) {
        logger.warn("Referenced customer not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    // Caller tried to act on a customerId that is not their own -> 403 Forbidden.
    @ExceptionHandler(UnauthorizedAccountAccessException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(UnauthorizedAccountAccessException ex, HttpServletRequest request) {
        logger.warn("Forbidden account access: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    // customer-service could not be reached to verify the customer -> fail closed with 503.
    @ExceptionHandler(CustomerServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleCustomerServiceDown(CustomerServiceUnavailableException ex, HttpServletRequest request) {
        logger.error("customer-service unavailable: {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    // 422 Unprocessable Entity: request was well-formed but an account business rule rejected it.
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex, HttpServletRequest request) {
        logger.warn("Account operation rejected: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidAccountOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidAccountOperationException ex, HttpServletRequest request) {
        logger.warn("Invalid account operation: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // A transfer that failed. If the debit was rolled back (reconciled) no money moved -> 409
    // Conflict (safe to retry). If rollback also failed the state is inconsistent -> 500.
    @ExceptionHandler(AccountTransferException.class)
    public ResponseEntity<ErrorResponse> handleTransferFailure(AccountTransferException ex, HttpServletRequest request) {
        HttpStatus status = ex.isReconciled() ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
        logger.error("Transfer failed (reconciled={}): {}", ex.isReconciled(), ex.getMessage());
        return build(status, ex.getMessage(), request);
    }

    // Rejects an unknown accountType string (AccountType.valueOf) and other bad arguments.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Invalid request argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    // Bean-validation failures on @Valid request bodies; field errors are flattened into the message.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        logger.warn("Validation failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
