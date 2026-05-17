package inholland.nl.banking_project_backend.exceptions;

public class InactiveAccountException extends RuntimeException {
    // Creates an inactive account error with a frontend-safe message.
    public InactiveAccountException(String message) {
        super(message);
    }
}
