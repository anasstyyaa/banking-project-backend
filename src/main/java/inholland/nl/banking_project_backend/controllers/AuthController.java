package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.ApiErrorDTO;
import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.exceptions.UserAlreadyExistsException;
import inholland.nl.banking_project_backend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for User Registration and Login")
public class AuthController {
    private final AuthService authService;

    // Registers a new customer and returns a JWT when registration succeeds.
    @Operation(summary = "Register a new user", description = "Creates a new customer account and returns a JWT token.")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO.RegisterRequest dto) {
        try {
            return ResponseEntity.status(201).body(authService.register(dto));
        } catch (UserAlreadyExistsException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed. Please try again.");
        }
    }

    // Authenticates a user and returns a JWT when credentials are valid.
    @Operation(summary = "Authenticate user", description = "Verifies credentials and returns a JWT token.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserDTO.LoginRequest dto) {
        try {
            return ResponseEntity.ok(authService.login(dto));
        } catch (BadCredentialsException exception) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Login failed. Please try again.");
        }
    }

    // Builds a consistent error response for authentication endpoints.
    private ResponseEntity<ApiErrorDTO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorDTO(message, LocalDateTime.now()));
    }
}
