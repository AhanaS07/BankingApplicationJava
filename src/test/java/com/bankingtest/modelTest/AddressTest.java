package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.banking.model.Address;

import org.junit.Before;
import org.junit.Test;

public class AddressTest {

    private Address address;

    @Before
    public void setUp() {
        address = new Address("1 Main St", "Pune", "MH", "411001");
    }

    @Test
    public void constructorStoresAllFields() {
        assertEquals("1 Main St", address.getStreet());
        assertEquals("Pune", address.getCity());
        assertEquals("MH", address.getState());
        assertEquals("411001", address.getPostalCode());
    }

    @Test
    public void settersUpdateFields() {
        address.setStreet("2 High St");
        address.setCity("Mumbai");
        address.setState("MH-2");
        address.setPostalCode("400001");
        assertEquals("2 High St", address.getStreet());
        assertEquals("Mumbai", address.getCity());
        assertEquals("MH-2", address.getState());
        assertEquals("400001", address.getPostalCode());
    }

    @Test
    public void cloneProducesEqualButDistinctInstance() throws CloneNotSupportedException {
        Address copy = address.clone();
        assertNotSame(address, copy);
        assertEquals(address.getStreet(), copy.getStreet());
        assertEquals(address.getCity(), copy.getCity());
        assertEquals(address.getState(), copy.getState());
        assertEquals(address.getPostalCode(), copy.getPostalCode());
    }

    @Test
    public void toStringContainsAllParts() {
        assertEquals("1 Main St, Pune, MH - 411001", address.toString());
    }
}
