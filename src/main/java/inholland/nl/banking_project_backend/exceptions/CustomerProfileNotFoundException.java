package inholland.nl.banking_project_backend.exceptions;

public class CustomerProfileNotFoundException extends RuntimeException {
    // Creates a missing customer profile error with a frontend-safe message.
    public CustomerProfileNotFoundException(String message) {
        super(message);
    }
}
