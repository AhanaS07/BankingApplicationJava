package com.bankingtest.exceptionTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.DuplicateCustomerException;

public class DuplicateCustomerExceptionTest {
    @Test
    public void testDuplicateCustomerException() {
        String errorMessage = "Customer already exists.";
        DuplicateCustomerException exception = assertThrows(DuplicateCustomerException.class, () -> {
            throw new DuplicateCustomerException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Customer already exists.";
        Throwable cause = new IllegalArgumentException("bad format");

        DuplicateCustomerException exception = new DuplicateCustomerException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
