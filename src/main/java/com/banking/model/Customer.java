package com.banking.model;

import com.banking.exception.InvalidEmailException;
import com.banking.exception.InvalidPhoneNumberException;

import java.util.Objects;
import java.util.regex.Pattern;

public class Customer implements Cloneable {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[6-9]\\d{9}$");

    private String customerId;
    private String name;
    private String email;
    private String phoneNumber;
    private Address address;

    public Customer(String customerId, String name, String email, String phoneNumber, Address address) {
        this.customerId = Objects.requireNonNull(customerId, "customerId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        setEmail(email);
        setPhoneNumber(phoneNumber);
        this.address = address;
    }

    public void validateEmail() {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidEmailException("Invalid email format: " + email);
        }
    }

    public void validatePhone() {
        if (phoneNumber == null || !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new InvalidPhoneNumberException("Invalid phone number: " + phoneNumber);
        }
    }

    public String getCustomerId() { return customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email;
        validateEmail();
    }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        validatePhone();
    }

    public Address getAddress() { return address; }
    public void setAddress(Address address) { this.address = address; }

    @Override
    public Customer clone() throws CloneNotSupportedException {
        Customer copy = (Customer) super.clone();
        if (this.address != null) {
            copy.address = this.address.clone();
        }
        return copy;
    }

    @Override
    public String toString() {
        return "Customer{id=" + customerId + ", name=" + name +
                ", email=" + email + ", phone=" + phoneNumber +
                ", address=" + address + "}";
    }
}
