package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.ErrorDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDTO.ErrorResponse> handleNotFound(EntityNotFoundException exception) {
        return ResponseEntity.status(404).body(new ErrorDTO.ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDTO.ErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorDTO.ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO.ErrorResponse> handleValidation() {
        return ResponseEntity.badRequest().body(new ErrorDTO.ErrorResponse("Invalid request"));
    }
}
