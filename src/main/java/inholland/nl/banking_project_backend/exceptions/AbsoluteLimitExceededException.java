package inholland.nl.banking_project_backend.exceptions;

public class AbsoluteLimitExceededException extends RuntimeException {
    // Creates an absolute limit error with a frontend-safe message.
    public AbsoluteLimitExceededException(String message) {
        super(message);
    }
}
