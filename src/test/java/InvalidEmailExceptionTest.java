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
}
