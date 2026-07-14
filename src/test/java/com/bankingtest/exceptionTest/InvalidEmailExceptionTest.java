package com.bankingtest.exceptionTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.InvalidEmailException;

public class InvalidEmailExceptionTest {
    @Test
    public void testInvalidEmailException() {
        String errorMessage = "Invalid email address.";
        InvalidEmailException exception = assertThrows(InvalidEmailException.class, () -> {
            throw new InvalidEmailException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Invalid email address.";
        Throwable cause = new IllegalArgumentException("bad format");

        InvalidEmailException exception = new InvalidEmailException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
