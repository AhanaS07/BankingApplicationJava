package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.banking.exception.InvalidEmailException;
import com.banking.exception.InvalidPhoneNumberException;
import com.banking.model.Address;
import com.banking.model.Customer;

import org.junit.Before;
import org.junit.Test;

public class CustomerTest {

    private Address address;

    @Before
    public void setUp() {
        address = new Address("1 Main St", "Pune", "MH", "411001");
    }

    private Customer newCustomer() {
        return new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", address);
    }

    @Test
    public void constructorStoresAllFields() {
        Customer c = newCustomer();
        assertEquals("CUS-1", c.getCustomerId());
        assertEquals("Asha", c.getName());
        assertEquals("asha@example.com", c.getEmail());
        assertEquals("9876543210", c.getPhoneNumber());
        assertSame(address, c.getAddress());
    }

    @Test
    public void constructorAcceptsNullAddress() {
        Customer c = new Customer("CUS-2", "Ravi", "ravi@example.com", "9876500000", null);
        assertNull(c.getAddress());
    }

    @Test
    public void constructorRejectsNullCustomerId() {
        assertThrows(NullPointerException.class,
                () -> new Customer(null, "Asha", "asha@example.com", "9876543210", address));
    }

    @Test
    public void constructorRejectsNullName() {
        assertThrows(NullPointerException.class,
                () -> new Customer("CUS-1", null, "asha@example.com", "9876543210", address));
    }

    @Test(expected = InvalidEmailException.class)
    public void constructorRejectsInvalidEmail() {
        new Customer("CUS-1", "Asha", "not-an-email", "9876543210", address);
    }

    @Test(expected = InvalidPhoneNumberException.class)
    public void constructorRejectsInvalidPhone() {
        new Customer("CUS-1", "Asha", "asha@example.com", "12345", address);
    }

    @Test(expected = InvalidEmailException.class)
    public void setEmailRejectsInvalidEmail() {
        newCustomer().setEmail("bad-email");
    }

    @Test
    public void setEmailAcceptsValidEmail() {
        Customer c = newCustomer();
        c.setEmail("new@example.com");
        assertEquals("new@example.com", c.getEmail());
    }

    @Test(expected = InvalidPhoneNumberException.class)
    public void setPhoneNumberRejectsInvalidPhone() {
        newCustomer().setPhoneNumber("0000000000");
    }

    @Test
    public void setPhoneNumberAcceptsValidPhone() {
        Customer c = newCustomer();
        c.setPhoneNumber("9998887776");
        assertEquals("9998887776", c.getPhoneNumber());
    }

    @Test
    public void setNameUpdatesName() {
        Customer c = newCustomer();
        c.setName("Asha Rao");
        assertEquals("Asha Rao", c.getName());
    }

    @Test
    public void setAddressUpdatesAddress() {
        Customer c = newCustomer();
        Address other = new Address("2 High St", "Mumbai", "MH", "400001");
        c.setAddress(other);
        assertSame(other, c.getAddress());
    }

    @Test
    public void cloneCopiesAddressDeeply() throws CloneNotSupportedException {
        Customer c = newCustomer();
        Customer copy = c.clone();
        assertEquals(c.getCustomerId(), copy.getCustomerId());
        assertNotSame(c.getAddress(), copy.getAddress());
        assertEquals(c.getAddress().getCity(), copy.getAddress().getCity());
    }

    @Test
    public void cloneHandlesNullAddress() throws CloneNotSupportedException {
        Customer c = new Customer("CUS-3", "Neha", "neha@example.com", "9876511111", null);
        Customer copy = c.clone();
        assertNull(copy.getAddress());
    }

    @Test
    public void toStringContainsKeyFields() {
        String text = newCustomer().toString();
        assertTrue(text.startsWith("Customer{"));
        assertTrue(text.contains("CUS-1"));
        assertTrue(text.contains("asha@example.com"));
    }
}
