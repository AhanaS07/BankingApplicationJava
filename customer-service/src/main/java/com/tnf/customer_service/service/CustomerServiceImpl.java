package com.tnf.customer_service.service;

import com.tnf.common_dto.dto.customer.AddressDto;
import com.tnf.common_dto.dto.customer.CustomerDto;
import com.tnf.customer_service.exception.CustomerNotFoundException;
import com.tnf.customer_service.exception.DuplicateCustomerException;
import com.tnf.customer_service.model.Address;
import com.tnf.customer_service.model.Customer;
import com.tnf.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    private final CustomerRepository customerRepository;

    @Override
    public CustomerDto createCustomer(CustomerDto customerDto) {
        if (customerRepository.existsByEmail(customerDto.getEmail())) {
            throw new DuplicateCustomerException("Customer already exists with email: " + customerDto.getEmail());
        }
        Customer entity = toEntity(customerDto);
        entity.setId(null);
        Customer saved = customerRepository.save(entity);
        log.info("Created customer with id {}", saved.getId());
        return toDto(saved);
    }

    @Override
    public CustomerDto getCustomerById(String id) {
        return toDto(findOrThrow(id));
    }

    @Override
    public List<CustomerDto> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public CustomerDto updateCustomer(String id, CustomerDto customerDto) {
        Customer existing = findOrThrow(id);
        existing.setFirstName(customerDto.getFirstName());
        existing.setLastName(customerDto.getLastName());
        existing.setEmail(customerDto.getEmail());
        existing.setPhone(customerDto.getPhone());
        existing.setAddress(toAddressEntity(customerDto.getAddress()));
        Customer updated = customerRepository.save(existing);
        log.info("Updated customer with id {}", updated.getId());
        return toDto(updated);
    }

    @Override
    public void deleteCustomer(String id) {
        Customer existing = findOrThrow(id);
        customerRepository.delete(existing);
        log.info("Deleted customer with id {}", id);
    }

    private Customer findOrThrow(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
    }

    private Customer toEntity(CustomerDto dto) {
        return new Customer(dto.getId(), dto.getFirstName(), dto.getLastName(), dto.getEmail(), dto.getPhone(),
                toAddressEntity(dto.getAddress()));
    }

    private CustomerDto toDto(Customer customer) {
        return new CustomerDto(customer.getId(), customer.getFirstName(), customer.getLastName(),
                customer.getEmail(), customer.getPhone(), toAddressDto(customer.getAddress()));
    }

    private Address toAddressEntity(AddressDto dto) {
        if (dto == null) return null;
        return new Address(dto.getLine1(), dto.getLine2(), dto.getCity(), dto.getState(), dto.getZip(), dto.getCountry());
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) return null;
        return new AddressDto(address.getLine1(), address.getLine2(), address.getCity(), address.getState(), address.getZip(), address.getCountry());
    }
}
