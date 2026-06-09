package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends BankingException {
    public UserAlreadyExistsException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
