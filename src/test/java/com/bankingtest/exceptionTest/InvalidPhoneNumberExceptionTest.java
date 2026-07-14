package com.bankingtest.exceptionTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.InvalidPhoneNumberException;

public class InvalidPhoneNumberExceptionTest {
    @Test
    public void testInvalidPhoneNumberException() {
        String errorMessage = "Invalid phone number.";
        InvalidPhoneNumberException exception = assertThrows(InvalidPhoneNumberException.class, () -> {
            throw new InvalidPhoneNumberException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Invalid phone number.";
        Throwable cause = new IllegalArgumentException("bad format");

        InvalidPhoneNumberException exception = new InvalidPhoneNumberException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
