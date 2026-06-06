package inholland.nl.banking_project_backend.exceptions;

public class UnauthorizedAccountAccessException extends RuntimeException {
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
