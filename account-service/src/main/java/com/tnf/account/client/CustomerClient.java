package com.tnf.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;

/**
 * Declarative client for customer-service, resolved by name through Eureka and load-balanced.
 *
 * <p>Used before creating an account to verify that the owning customer actually exists, since
 * there is no cross-service foreign key between accountdb and customerdb.
 */
@FeignClient(name = "customer-service", path = "/api/customers")
public interface CustomerClient {

    /**
     * Fetches a customer profile by id.
     *
     * @return customer-service's standard envelope; a 404 is raised as {@code FeignException.NotFound}
     */
    @GetMapping("/{id}")
    ApiResponse<CustomerDto> getCustomer(@PathVariable("id") String id);
}
