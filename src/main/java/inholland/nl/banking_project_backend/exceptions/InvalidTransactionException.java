package inholland.nl.banking_project_backend.exceptions;

public class InvalidTransactionException extends RuntimeException {
    // Creates an invalid transaction error with a frontend-safe message.
    public InvalidTransactionException(String message) {
        super(message);
    }
}
