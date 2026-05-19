package exception;

public class WalletLimitExceededException extends Exception{
    public WalletLimitExceededException(String message) { 
        super(message); 
    }
}
