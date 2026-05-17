package inholland.nl.banking_project_backend.exceptions;

public class AccountNotFoundException extends RuntimeException {
    // Creates an account not found error with a frontend-safe message.
    public AccountNotFoundException(String message) {
        super(message);
    }
}
