package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedAccountAccessException extends BankingException {
    public UnauthorizedAccountAccessException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}