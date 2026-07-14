package com.bankingtest.exceptionTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.InsufficientBalanceException;

public class InsufficientBalanceExceptionTest {
    @Test
    public void testInsufficientBalanceException() {
        String errorMessage = "Insufficient balance.";
        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> {
            throw new InsufficientBalanceException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Insufficient balance.";
        Throwable cause = new IllegalArgumentException("bad format");

        InsufficientBalanceException exception = new InsufficientBalanceException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
