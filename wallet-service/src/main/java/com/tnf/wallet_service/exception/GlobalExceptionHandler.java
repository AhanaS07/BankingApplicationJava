package com.tnf.wallet_service.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Exceptions to meaningful HTTP responses.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(WalletNotFoundException ex) {
        logger.warn("Wallet not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ InvalidAmountException.class, InsufficientBalanceException.class,
            WalletLimitExceededException.class })
    // 422 Unprocessable Entity: request was well-formed but a wallet business rule rejected it.
    public ResponseEntity<Map<String, Object>> handleBusinessRule(RuntimeException ex) {
        logger.warn("Wallet operation rejected: {}", ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    // A transfer that failed. If the debit was rolled back (reconciled) no money moved -> 409
    // Conflict (safe to retry). If rollback also failed the state is inconsistent -> 500.
    @ExceptionHandler(WalletTransferException.class)
    public ResponseEntity<Map<String, Object>> handleTransferFailure(WalletTransferException ex) {
        HttpStatus status = ex.isReconciled() ? HttpStatus.CONFLICT : HttpStatus.INTERNAL_SERVER_ERROR;
        logger.error("Transfer failed (reconciled={}): {}", ex.isReconciled(), ex.getMessage());
        return build(status, ex.getMessage());
    }

    // Rejects an unknown walletType string (WalletType.valueOf) and other bad arguments.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Invalid request argument: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // Bean-validation failures on @Valid request bodies.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = baseBody(HttpStatus.BAD_REQUEST);
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        logger.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = baseBody(status);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> baseBody(HttpStatus status) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        return body;
    }
}
