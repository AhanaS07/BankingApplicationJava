package com.tnf.customer_service.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.tnf.common_dto.dto.common.ApiResponse;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.service.CustomerService;


class CustomerControllerTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    private CustomerDto customer(String id) {
        CustomerDto dto = new CustomerDto();
        dto.setId(id);
        return dto;
    }

    @Test
    void updateCustomer_delegatesToServiceAndReturnsUpdatedDto_whenOwnerUpdatesOwnProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service, INTERNAL_KEY);
        CustomerDto request = customer("me");
        when(service.updateCustomer("me", request)).thenReturn(customer("me"));

        ResponseEntity<ApiResponse<CustomerDto>> response = controller.updateCustomer("me", request, "me");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("me", response.getBody().getData().getId());
        verify(service).updateCustomer("me", request);
    }

    @Test
    void deleteCustomer_delegatesToServiceAndReturns200_whenOwnerDeletesOwnProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service, INTERNAL_KEY);

        ResponseEntity<ApiResponse<Void>> response = controller.deleteCustomer("me", "me");

        assertEquals(200, response.getStatusCode().value());
        verify(service).deleteCustomer("me");
    }
}
