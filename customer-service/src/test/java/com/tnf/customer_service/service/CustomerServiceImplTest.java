package com.tnf.customer_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.tnf.common_dto.dto.customer.AddressDto;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.exception.CustomerNotFoundException;
import com.tnf.customer_service.exception.DuplicateCustomerException;
import com.tnf.customer_service.model.Address;
import com.tnf.customer_service.model.Customer;
import com.tnf.customer_service.repository.CustomerRepository;

class CustomerServiceImplTest {

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerServiceImpl service = new CustomerServiceImpl(repository);

    private CustomerDto customerDto(String email) {
        CustomerDto dto = new CustomerDto();
        dto.setFirstName("Jane");
        dto.setLastName("Doe");
        dto.setEmail(email);
        dto.setPhone("9876543210");
        return dto;
    }

    @Test
    void createCustomer_savesAndReturnsDto_whenEmailNotAlreadyUsed() {
        CustomerDto request = customerDto("jane@example.com");
        when(repository.existsByEmail("jane@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> {
            Customer saved = invocation.getArgument(0);
            saved.setId("generated-id");
            return saved;
        });

        CustomerDto result = service.createCustomer(request);

        assertEquals("generated-id", result.getId());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void createCustomer_throwsDuplicateCustomerException_whenEmailAlreadyUsed() {
        CustomerDto request = customerDto("jane@example.com");
        when(repository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThrows(DuplicateCustomerException.class, () -> service.createCustomer(request));
        verify(repository, never()).save(any());
    }

    // Regression test for a fixed bug: a client-supplied id in the create request must never reach
    // save(), or Mongo would treat it as an upsert and silently overwrite an existing customer.
    @Test
    void createCustomer_ignoresClientSuppliedId_soAnExistingCustomerCannotBeOverwritten() {
        CustomerDto request = customerDto("jane@example.com");
        request.setId("someone-elses-id");
        when(repository.existsByEmail("jane@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);

        service.createCustomer(request);

        verify(repository).save(savedCustomer.capture());
        assertNull(savedCustomer.getValue().getId());
    }

    @Test
    void createCustomer_mapsPopulatedAddress() {
        CustomerDto request = customerDto("jane@example.com");
        AddressDto address = new AddressDto();
        address.setLine1("221B Baker St");
        address.setCity("London");
        address.setState("London");
        address.setZip("NW1 6XE");
        address.setCountry("UK");
        request.setAddress(address);
        when(repository.existsByEmail("jane@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerDto result = service.createCustomer(request);

        assertEquals("221B Baker St", result.getAddress().getLine1());
        assertEquals("UK", result.getAddress().getCountry());
    }

    @Test
    void createCustomer_mapsNullAddress_withoutThrowing() {
        CustomerDto request = customerDto("jane@example.com");
        request.setAddress(null);
        when(repository.existsByEmail("jane@example.com")).thenReturn(false);
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerDto result = service.createCustomer(request);

        assertNull(result.getAddress());
    }

    @Test
    void getCustomerById_returnsMappedDto_whenFound() {
        Customer entity = new Customer("id-1", "Jane", "Doe", "jane@example.com", "9876543210", null);
        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        CustomerDto result = service.getCustomerById("id-1");

        assertEquals("id-1", result.getId());
        assertEquals("jane@example.com", result.getEmail());
    }

    @Test
    void getCustomerById_mapsAddress_whenPresent() {
        Address address = new Address("221B Baker St", null, "London", "London", "NW1 6XE", "UK");
        Customer entity = new Customer("id-1", "Jane", "Doe", "jane@example.com", "9876543210", address);
        when(repository.findById("id-1")).thenReturn(Optional.of(entity));

        CustomerDto result = service.getCustomerById("id-1");

        assertEquals("221B Baker St", result.getAddress().getLine1());
        assertEquals("UK", result.getAddress().getCountry());
    }

    @Test
    void getCustomerById_throwsCustomerNotFoundException_whenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.getCustomerById("missing"));
    }

    @Test
    void getAllCustomers_returnsAllMappedToDto() {
        Customer c1 = new Customer("id-1", "Jane", "Doe", "jane@example.com", "9876543210", null);
        Customer c2 = new Customer("id-2", "John", "Smith", "john@example.com", "9123456780", null);
        when(repository.findAll()).thenReturn(List.of(c1, c2));

        List<CustomerDto> result = service.getAllCustomers();

        assertEquals(2, result.size());
        assertEquals("id-1", result.get(0).getId());
        assertEquals("id-2", result.get(1).getId());
    }

    @Test
    void getAllCustomers_returnsEmptyList_whenNoneExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertTrue(service.getAllCustomers().isEmpty());
    }

    @Test
    void updateCustomer_updatesFieldsAndSaves_whenFound() {
        Customer existing = new Customer("id-1", "Jane", "Doe", "jane@example.com", "9876543210", null);
        when(repository.findById("id-1")).thenReturn(Optional.of(existing));
        when(repository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CustomerDto update = customerDto("jane.new@example.com");
        update.setFirstName("Janet");

        CustomerDto result = service.updateCustomer("id-1", update);

        assertEquals("id-1", result.getId());
        assertEquals("Janet", result.getFirstName());
        assertEquals("jane.new@example.com", result.getEmail());
    }

    @Test
    void updateCustomer_throwsCustomerNotFoundException_whenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        CustomerDto update = customerDto("jane@example.com");

        assertThrows(CustomerNotFoundException.class, () -> service.updateCustomer("missing", update));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteCustomer_deletesEntity_whenFound() {
        Customer existing = new Customer("id-1", "Jane", "Doe", "jane@example.com", "9876543210", null);
        when(repository.findById("id-1")).thenReturn(Optional.of(existing));

        service.deleteCustomer("id-1");

        verify(repository).delete(existing);
    }

    @Test
    void deleteCustomer_throwsCustomerNotFoundException_whenMissing() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.deleteCustomer("missing"));
        verify(repository, never()).delete(any());
    }
}
