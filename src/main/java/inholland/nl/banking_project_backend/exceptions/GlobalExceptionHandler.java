package inholland.nl.banking_project_backend.exceptions;

import inholland.nl.banking_project_backend.dtos.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handles validation errors (@NotBlank, @Email, @Size)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String fieldMessages = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        String objectMessages = ex.getBindingResult().getGlobalErrors()
                .stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        String message = java.util.stream.Stream.of(fieldMessages, objectMessages)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("; "));

        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    // handles custom business logic
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getStatusCode().value(),
                ex.getReason(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // handles "not found"
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // handles authentication failures (invalid email/password)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ignored) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid email or password.",
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // handles missing account errors from account and transaction services
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccountNotFound(AccountNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // handles inactive account errors from account and transaction services
    @ExceptionHandler(InactiveAccountException.class)
    public ResponseEntity<ErrorResponseDTO> handleInactiveAccount(InactiveAccountException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // handles account ownership and authorization business errors
    @ExceptionHandler(UnauthorizedAccountAccessException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorizedAccountAccess(UnauthorizedAccountAccessException ex) {
        return buildError(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // handles invalid transaction request data
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidTransaction(InvalidTransactionException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // handles invalid account state changes
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    // handles absolute and daily limit errors
    @ExceptionHandler({AbsoluteLimitExceededException.class, DailyLimitExceededException.class})
    public ResponseEntity<ErrorResponseDTO> handleLimitExceeded(RuntimeException ex) {
        return buildError(HttpStatusCode.valueOf(422), ex.getMessage());
    }

    // creates the shared error response body for service-layer exceptions
    private ResponseEntity<ErrorResponseDTO> buildError(HttpStatusCode status, String message) {
        ErrorResponseDTO error = new ErrorResponseDTO(status.value(), message, LocalDateTime.now());
        return new ResponseEntity<>(error, status);
    }

    // Formats one validation error for the shared error response message.
    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
