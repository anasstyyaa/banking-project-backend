package inholland.nl.banking_project_backend.exceptions;

public class LimitExceededException extends RuntimeException {
    public LimitExceededException(String message) { super(message); }
}
