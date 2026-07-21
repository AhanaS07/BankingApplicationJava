package com.tnf.api_gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/fallback")
public class FallBackController {

    private static final Logger logger = LoggerFactory.getLogger(FallBackController.class);

    @GetMapping("/wallets")
    public ResponseEntity<Map<String, String>> walletServiceFallback() {
        // A fallback firing means the wallet-service circuit breaker is open or the call timed out.
        logger.warn("Circuit-breaker fallback triggered for wallet-service; returning 503");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wallet service is currently unavailable. Please try again later.");
        response.put("status", "503 Service Unavailable");
        return ResponseEntity.status(503).body(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, String>> accountServiceFallback() {
        logger.warn("Circuit-breaker fallback triggered for account-service; returning 503");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account service is currently unavailable. Please try again later.");
        response.put("status", "503 Service Unavailable");
        return ResponseEntity.status(503).body(response);
    }

    @GetMapping("/customers")
    public ResponseEntity<Map<String, String>> customerServiceFallback() {
        logger.warn("Circuit-breaker fallback triggered for customer-service; returning 503");
        Map<String, String> response = new HashMap<>();
        response.put("message", "Customer service is currently unavailable. Please try again later.");
        response.put("status", "503 Service Unavailable");
        return ResponseEntity.status(503).body(response);
    }

}
