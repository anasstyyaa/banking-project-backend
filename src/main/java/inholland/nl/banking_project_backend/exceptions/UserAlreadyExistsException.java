package inholland.nl.banking_project_backend.exceptions;

public class UserAlreadyExistsException extends RuntimeException {
    // Creates a duplicate user error with a frontend-safe message.
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
