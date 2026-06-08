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
}
