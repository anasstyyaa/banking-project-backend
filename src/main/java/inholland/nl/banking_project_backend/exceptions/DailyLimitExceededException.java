package inholland.nl.banking_project_backend.exceptions;

public class DailyLimitExceededException extends RuntimeException {
    // Creates a daily limit error with a frontend-safe message.
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
