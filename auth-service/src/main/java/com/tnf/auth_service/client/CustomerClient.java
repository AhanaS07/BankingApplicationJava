package com.tnf.auth_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;

/**
 * Declarative client for customer-service, resolved by name through Eureka and load-balanced.
 *
 * <p>Used at registration time to create the customer profile that a new auth user is linked to.
 */
@FeignClient(name = "customer-service", path = "/api/customers")
public interface CustomerClient {

    /**
     * Creates a customer profile.
     *
     * @return customer-service's standard envelope; {@code data.id} is the new customerId
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<CustomerDto> createCustomer(@RequestBody CustomerDto customer);
}
