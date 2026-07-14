package com.bankingtest.modelTest;

import static org.junit.Assert.assertEquals;

import com.banking.model.AccountType;

import org.junit.Test;

public class AccountTypeTest {

    @Test
    public void valuesContainsAllConstants() {
        assertEquals(2, AccountType.values().length);
    }

    @Test
    public void valueOfResolvesConstants() {
        assertEquals(AccountType.SAVINGS, AccountType.valueOf("SAVINGS"));
        assertEquals(AccountType.CURRENT, AccountType.valueOf("CURRENT"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void valueOfRejectsUnknownConstant() {
        AccountType.valueOf("UNKNOWN");
    }
}
