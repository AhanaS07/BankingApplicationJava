package com.tnf.customer_service.service;

import com.tnf.common_dto.dto.customer.CustomerDto;

import java.util.List;

public interface CustomerService {
    CustomerDto createCustomer(CustomerDto customerDto);
    CustomerDto getCustomerById(String id);
    List<CustomerDto> getAllCustomers();
    CustomerDto updateCustomer(String id, CustomerDto customerDto);
    void deleteCustomer(String id);
}

