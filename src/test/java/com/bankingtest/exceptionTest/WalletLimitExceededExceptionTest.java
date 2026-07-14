package com.bankingtest.exceptionTest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import org.junit.Test;
import com.banking.exception.WalletLimitExceededException;

public class WalletLimitExceededExceptionTest {
    @Test
    public void testWalletLimitExceededException() {
        String errorMessage = "Wallet limit exceeded.";
        WalletLimitExceededException exception = assertThrows(WalletLimitExceededException.class, () -> {
            throw new WalletLimitExceededException(errorMessage);
        });

        // Verify that the exception message is correct
        assert(exception.getMessage().equals(errorMessage));
    }

     @Test
    public void testMessageAndCauseConstructor() {
        String errorMessage = "Wallet limit exceeded.";
        Throwable cause = new IllegalArgumentException("bad format");

        WalletLimitExceededException exception = new WalletLimitExceededException(errorMessage, cause);

        assertEquals(errorMessage, exception.getMessage());   // checks super(message,...)
        assertSame(cause, exception.getCause());              // checks super(..., cause)  ← this line covers the 2-arg ctor
    }
}
