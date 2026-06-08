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
}
