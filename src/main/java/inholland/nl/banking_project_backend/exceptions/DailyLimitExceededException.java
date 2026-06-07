package inholland.nl.banking_project_backend.exceptions;

public class DailyLimitExceededException extends LimitExceededException {
    public DailyLimitExceededException(String message) { super(message); }
}
