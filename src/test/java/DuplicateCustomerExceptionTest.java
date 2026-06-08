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
}
