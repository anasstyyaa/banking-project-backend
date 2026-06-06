package inholland.nl.banking_project_backend.exceptions;

public class DailyLimitExceededException extends RuntimeException {
    public DailyLimitExceededException(String message) {
        super(message);
    }
}
