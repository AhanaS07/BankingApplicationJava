package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.banking.exception.InsufficientBalanceException;
import com.banking.model.CurrentAccount;
import com.banking.model.Customer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

public class CurrentAccountTest {

    private static final double DELTA = 1e-9;

    private Customer customer;

    @Before
    public void setUp() {
        customer = new Customer("CUS-1", "Asha", "asha@example.com", "9876543210", null);
    }

    @Test
    public void constructorStoresOverdraftLimit() {
        CurrentAccount acc = new CurrentAccount("CUR-1", customer, 1_000.0, 500.0);
        assertEquals(500.0, acc.getOverdraftLimit(), DELTA);
        assertEquals(1_000.0, acc.getBalance(), DELTA);
    }

    @Test
    public void calculateInterestIsAlwaysZero() {
        CurrentAccount acc = new CurrentAccount("CUR-1", customer, 5_000.0, 500.0);
        assertEquals(0.0, acc.calculateInterest(), DELTA);
    }

    @Test
    public void withdrawAllowedIntoOverdraft() {
        CurrentAccount acc = new CurrentAccount("CUR-1", customer, 100.0, 500.0);
        acc.withdraw(400.0);
        assertEquals(-300.0, acc.getBalance(), DELTA);
    }

    @Test
    public void withdrawAllowedUpToExactOverdraftLimit() {
        CurrentAccount acc = new CurrentAccount("CUR-1", customer, 100.0, 500.0);
        acc.withdraw(600.0);
        assertEquals(-500.0, acc.getBalance(), DELTA);
    }

    @Test(expected = InsufficientBalanceException.class)
    public void withdrawRejectsBeyondOverdraftLimit() {
        CurrentAccount acc = new CurrentAccount("CUR-1", customer, 100.0, 500.0);
        acc.withdraw(601.0);
    }

    @Test
    public void displayDetailsPrintsAccountSummary() {
        CurrentAccount acc = new CurrentAccount("CUR-9", customer, 2_000.0, 1_000.0);
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            acc.displayDetails();
        } finally {
            System.setOut(originalOut);
        }
        String output = captured.toString();
        assertTrue(output.contains("[Current]"));
        assertTrue(output.contains("CUR-9"));
        assertTrue(output.contains("Asha"));
    }
}
