package inholland.nl.banking_project_backend.exceptions;

import org.springframework.http.HttpStatus;

// common base for all domain/business exceptions — each subclass declares the HTTP status it should map to
public abstract class BankingException extends RuntimeException {
    private final HttpStatus status;

    protected BankingException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}