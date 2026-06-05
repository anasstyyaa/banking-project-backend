package inholland.nl.banking_project_backend.exceptions;

import inholland.nl.banking_project_backend.dtos.ErrorResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    //validation errors (@NotBlank, @Email, @Pattern, etc)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String fieldMessages = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        String objectMessages = ex.getBindingResult().getGlobalErrors()
                .stream()
                .map(org.springframework.validation.ObjectError::getDefaultMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("; "));

        String message = Stream.of(fieldMessages, objectMessages)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining("; "));

        return buildError(HttpStatus.BAD_REQUEST, message);
    }

    //custom ResponseStatusException logic
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(
                ex.getStatusCode().value(),
                ex.getReason(),
                LocalDateTime.now()
        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    //missing database resources
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    //authentication failures (invalid email/password)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleBadCredentials(BadCredentialsException ignored) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
    }

    //invalid request and business input
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    //invalid account state changes
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalState(IllegalStateException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    //banking transaction limit overages
    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleLimitExceeded(LimitExceededException ex) {
        return buildError(HttpStatusCode.valueOf(422), ex.getMessage());
    }

    //catch-all fallback
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleAllUncaughtExceptions(Exception ex) {
        // Log the actual stack trace internally so you can debug behind the scenes
        log.error("An unhandled system exception occurred within the application pipeline:", ex);

        // Return a generic 500 error matching your exact ErrorResponseDTO structure
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred. Please contact system administrators."
        );
    }


    private ResponseEntity<ErrorResponseDTO> buildError(HttpStatusCode status, String message) {
        ErrorResponseDTO error = new ErrorResponseDTO(status.value(), message, LocalDateTime.now());
        return new ResponseEntity<>(error, status);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}