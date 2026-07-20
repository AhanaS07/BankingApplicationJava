package com.tnf.api_gateway.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/fallback")
public class FallBackController {

    @GetMapping("/wallets")
    public ResponseEntity<Map<String, String>> walletServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Wallet service is currently unavailable. Please try again later.");
        response.put("status", "503 Service Unavailable");
        return ResponseEntity.status(503).body(response);
    }

    @GetMapping("/accounts")
    public ResponseEntity<Map<String, String>> accountServiceFallback() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Account service is currently unavailable. Please try again later.");
        response.put("status", "503 Service Unavailable");
        return ResponseEntity.status(503).body(response);
    }

}
