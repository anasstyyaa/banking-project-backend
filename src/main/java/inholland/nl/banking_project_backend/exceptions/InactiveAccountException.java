package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class InactiveAccountException extends BankingException {
    public InactiveAccountException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}