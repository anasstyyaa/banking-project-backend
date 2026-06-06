package inholland.nl.banking_project_backend.exceptions;

public class AbsoluteLimitExceededException extends RuntimeException {
    public AbsoluteLimitExceededException(String message) {
        super(message);
    }
}
