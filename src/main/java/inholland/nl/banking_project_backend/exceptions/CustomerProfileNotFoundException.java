package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class CustomerProfileNotFoundException extends BankingException {
    public CustomerProfileNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}