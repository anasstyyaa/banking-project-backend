package inholland.nl.banking_project_backend.exceptions;

public class InsufficientFundsException extends RuntimeException {
    // Creates an insufficient funds error with a frontend-safe message.
    public InsufficientFundsException(String message) {
        super(message);
    }
}
