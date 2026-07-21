package com.tnf.customer_service.controller;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.exception.UnauthorizedCustomerAccessException;
import com.tnf.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final String internalApiKey;

    public CustomerController(CustomerService customerService,
            @Value("${security.internal.api-key}") String internalApiKey) {
        this.customerService = customerService;
        this.internalApiKey = internalApiKey;
    }

    // Internal-only: this is the provisioning entry point called by auth-service (via Feign) during
    // registration, before the user has a customerId to own. It carries no X-Auth-Customer-Id (the
    // register call is unauthenticated), so it cannot use the ownership check the other endpoints do.
    // Instead the caller must present the shared internal API key, which only trusted services hold
    // and which the gateway strips from any inbound request — so no external, JWT-bearing client can
    // reach it, even though the /api/customers route itself requires a token.
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CustomerDto customerDto,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String internalApiKey) {
        requireInternalCaller(internalApiKey);
        CustomerDto created = customerService.createCustomer(customerDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer created successfully", created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> getCustomer(@PathVariable String id,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        requireOwnership(authCustomerId, id);
        return ResponseEntity.ok(ApiResponse.success("Customer fetched successfully", customerService.getCustomerById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDto>>> getAllCustomers(
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        // Scoped to the caller: returns only the authenticated customer's own profile,
        // never every customer in the system.
        requireAuthenticated(authCustomerId);
        return ResponseEntity.ok(ApiResponse.success("Customers fetched successfully",
                List.of(customerService.getCustomerById(authCustomerId))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDto>> updateCustomer(@PathVariable String id,
            @Valid @RequestBody CustomerDto customerDto,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        requireOwnership(authCustomerId, id);
        return ResponseEntity.ok(ApiResponse.success("Customer updated successfully", customerService.updateCustomer(id, customerDto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String id,
            @RequestHeader(value = "X-Auth-Customer-Id", required = false) String authCustomerId) {
        requireOwnership(authCustomerId, id);
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success("Customer deleted successfully", null));
    }

    // A caller may only act on their OWN customer profile. The gateway validates the JWT and injects
    // the authenticated customerId as X-Auth-Customer-Id; account/wallet services propagate the same
    // header on their internal lookups. Any request whose id differs (or that lacks the header, i.e.
    // did not come through the gateway) is rejected.
    private void requireOwnership(String authCustomerId, String requestedCustomerId) {
        requireAuthenticated(authCustomerId);
        if (!authCustomerId.equals(requestedCustomerId)) {
            throw new UnauthorizedCustomerAccessException("You may only access your own customer profile");
        }
    }

    private void requireAuthenticated(String authCustomerId) {
        if (authCustomerId == null || authCustomerId.isBlank()) {
            throw new UnauthorizedCustomerAccessException(
                    "Missing authenticated customer identity; requests must go through the API gateway");
        }
    }

    // Provisioning is restricted to trusted internal services (auth-service) that hold the shared
    // key. Compared in constant time so a mismatch can't be inferred from response timing.
    private void requireInternalCaller(String providedKey) {
        if (providedKey == null || !MessageDigest.isEqual(
                providedKey.getBytes(StandardCharsets.UTF_8),
                internalApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new UnauthorizedCustomerAccessException(
                    "Customer provisioning is restricted to internal services");
        }
    }
}
