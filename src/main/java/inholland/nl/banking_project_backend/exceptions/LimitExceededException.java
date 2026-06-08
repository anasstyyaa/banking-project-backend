package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class LimitExceededException extends BankingException {
    public LimitExceededException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, message);
    }
    
    protected LimitExceededException(HttpStatus status, String message) {
        super(status, message);
    }
}