package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTransactionException extends BankingException {
    public InvalidTransactionException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}