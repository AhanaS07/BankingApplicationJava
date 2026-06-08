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
}
