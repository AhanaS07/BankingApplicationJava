package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.banking.model.Customer;
import com.banking.model.SavingsAccount;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

public class SavingsAccountTest {

    private static final double DELTA = 1e-9;

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", null);
    }

    @Test
    public void constructorStoresInterestRate() {
        SavingsAccount acc = new SavingsAccount("SAV-1", customer, 1_000.0, 4.5);
        assertEquals(4.5, acc.getInterestRate(), DELTA);
        assertEquals(1_000.0, acc.getBalance(), DELTA);
    }

    @Test
    public void calculateInterestUsesBalanceAndRate() {
        SavingsAccount acc = new SavingsAccount("SAV-1", customer, 2_000.0, 5.0);
        assertEquals(100.0, acc.calculateInterest(), DELTA);
    }

    @Test
    public void calculateInterestIsZeroForZeroBalance() {
        SavingsAccount acc = new SavingsAccount("SAV-1", customer, 0.0, 5.0);
        assertEquals(0.0, acc.calculateInterest(), DELTA);
    }

    @Test
    public void displayDetailsPrintsAccountSummary() {
        SavingsAccount acc = new SavingsAccount("SAV-9", customer, 1_500.0, 3.0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            acc.displayDetails();
        } finally {
            System.setOut(originalOut);
        }
        String output = captured.toString();
        assertTrue(output.contains("[Savings]"));
        assertTrue(output.contains("SAV-9"));
        assertTrue(output.contains("Asha"));
    }
}
