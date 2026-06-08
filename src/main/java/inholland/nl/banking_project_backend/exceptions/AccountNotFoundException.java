package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends BankingException {
    public AccountNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}