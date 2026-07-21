package com.tnf.customer_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.controller.CustomerController;
import com.tnf.customer_service.exception.UnauthorizedCustomerAccessException;
import com.tnf.customer_service.service.CustomerService;

/**
 * Unit tests for ownership enforcement on customer-service. The customer's id IS the resource id,
 * so ownership means: you may only act on the customer whose id equals your X-Auth-Customer-Id.
 * POST stays open (registration provisioning path).
 */
class CustomerOwnershipTest {

    private CustomerDto customer(String id) {
        CustomerDto c = new CustomerDto();
        c.setId(id);
        return c;
    }

    @Test
    void getCustomer_allowsReadingOwnProfile() {
        CustomerService service = mock(CustomerService.class);
        when(service.getCustomerById("me")).thenReturn(customer("me"));
        CustomerController controller = new CustomerController(service);
        assertEquals(200, controller.getCustomer("me", "me").getStatusCode().value());
    }

    @Test
    void getCustomer_forbidsReadingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.getCustomer("victim", "attacker"));
        verify(service, never()).getCustomerById(any());
    }

    @Test
    void getCustomer_forbidsWhenNoIdentityHeader() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.getCustomer("me", null));
    }

    @Test
    void updateCustomer_forbidsUpdatingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service);
        assertThrows(UnauthorizedCustomerAccessException.class,
                () -> controller.updateCustomer("victim", customer("victim"), "attacker"));
        verify(service, never()).updateCustomer(any(), any());
    }

    @Test
    void deleteCustomer_forbidsDeletingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = new CustomerController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.deleteCustomer("victim", "attacker"));
        verify(service, never()).deleteCustomer(any());
    }

    @Test
    void getAllCustomers_isScopedToCaller_notEveryCustomer() {
        CustomerService service = mock(CustomerService.class);
        when(service.getCustomerById("me")).thenReturn(customer("me"));
        CustomerController controller = new CustomerController(service);
        controller.getAllCustomers("me");
        verify(service).getCustomerById("me");
        verify(service, never()).getAllCustomers();
    }

    @Test
    void createCustomer_staysOpen_forRegistrationProvisioning() {
        CustomerService service = mock(CustomerService.class);
        when(service.createCustomer(any())).thenReturn(customer("new-id"));
        CustomerController controller = new CustomerController(service);
        // No X-Auth-Customer-Id header (this is the auth-service Feign provisioning call) -> still succeeds.
        assertEquals(201, controller.createCustomer(customer(null)).getStatusCode().value());
        verify(service).createCustomer(any());
    }
}
