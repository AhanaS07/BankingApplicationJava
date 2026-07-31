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
 * POST is the internal provisioning path, gated by the shared internal API key.
 */
class CustomerOwnershipTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    private CustomerController newController(CustomerService service) {
        return new CustomerController(service, INTERNAL_KEY);
    }

    private CustomerDto customer(String id) {
        CustomerDto c = new CustomerDto();
        c.setId(id);
        return c;
    }

    @Test
    void getCustomer_allowsReadingOwnProfile() {
        CustomerService service = mock(CustomerService.class);
        when(service.getCustomerById("me")).thenReturn(customer("me"));
        CustomerController controller = newController(service);
        assertEquals(200, controller.getCustomer("me", "me").getStatusCode().value());
    }

    @Test
    void getCustomer_forbidsReadingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.getCustomer("victim", "attacker"));
        verify(service, never()).getCustomerById(any());
    }

    @Test
    void getCustomer_forbidsWhenNoIdentityHeader() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.getCustomer("me", null));
    }

    @Test
    void getCustomer_forbidsWhenIdentityHeaderIsBlank() {
        // A blank header (e.g. "X-Auth-Customer-Id: ") must be rejected the same as a missing one.
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.getCustomer("me", "  "));
    }

    @Test
    void updateCustomer_forbidsUpdatingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class,
                () -> controller.updateCustomer("victim", customer("victim"), "attacker"));
        verify(service, never()).updateCustomer(any(), any());
    }

    @Test
    void deleteCustomer_forbidsDeletingAnotherProfile() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class, () -> controller.deleteCustomer("victim", "attacker"));
        verify(service, never()).deleteCustomer(any());
    }

    @Test
    void getAllCustomers_isScopedToCaller_notEveryCustomer() {
        CustomerService service = mock(CustomerService.class);
        when(service.getCustomerById("me")).thenReturn(customer("me"));
        CustomerController controller = newController(service);
        controller.getAllCustomers("me");
        verify(service).getCustomerById("me");
        verify(service, never()).getAllCustomers();
    }

    @Test
    void createCustomer_succeedsWithValidInternalKey() {
        CustomerService service = mock(CustomerService.class);
        when(service.createCustomer(any())).thenReturn(customer("new-id"));
        CustomerController controller = newController(service);
        // The auth-service Feign provisioning call presents the shared internal key -> succeeds.
        assertEquals(201, controller.createCustomer(customer(null), INTERNAL_KEY).getStatusCode().value());
        verify(service).createCustomer(any());
    }

    @Test
    void createCustomer_forbidsExternalCallerWithoutInternalKey() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        // An external, JWT-bearing client cannot supply the key (the gateway strips it) -> rejected.
        assertThrows(UnauthorizedCustomerAccessException.class,
                () -> controller.createCustomer(customer(null), null));
        verify(service, never()).createCustomer(any());
    }

    @Test
    void createCustomer_forbidsWrongInternalKey() {
        CustomerService service = mock(CustomerService.class);
        CustomerController controller = newController(service);
        assertThrows(UnauthorizedCustomerAccessException.class,
                () -> controller.createCustomer(customer(null), "wrong-key"));
        verify(service, never()).createCustomer(any());
    }
}
