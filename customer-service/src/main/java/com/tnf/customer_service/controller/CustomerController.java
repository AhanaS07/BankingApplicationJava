package com.tnf.customer_service.controller;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.exception.UnauthorizedCustomerAccessException;
import com.tnf.customer_service.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    // Open by design: this is the provisioning entry point called by auth-service (via Feign) during
    // registration, before the user has a customerId to own. It is not exposed as a public route at
    // the gateway, so external callers still need a valid token to reach it.
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto>> createCustomer(@Valid @RequestBody CustomerDto customerDto) {
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
}
