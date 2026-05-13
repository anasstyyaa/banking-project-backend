package inholland.nl.banking_project_backend.exceptions;

public class UnauthorizedAccountAccessException extends RuntimeException {
    // Creates an unauthorized account access error with a frontend-safe message.
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
