package com.bankingtest.exceptionTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.InvalidAmountException;

public class InvalidAmountExceptionTest {
    @Test
    public void testInvalidAmountException() {
        String errorMessage = "Invalid amount.";
        InvalidAmountException exception = assertThrows(InvalidAmountException.class, () -> {
            throw new InvalidAmountException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Invalid amount.";
        Throwable cause = new IllegalArgumentException("bad format");

        InvalidAmountException exception = new InvalidAmountException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
