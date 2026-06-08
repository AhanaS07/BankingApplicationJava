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
}
