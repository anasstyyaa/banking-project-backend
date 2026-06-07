package inholland.nl.banking_project_backend.exceptions;

public class AbsoluteLimitExceededException extends LimitExceededException {
    public AbsoluteLimitExceededException(String message) { super(message); }
}
