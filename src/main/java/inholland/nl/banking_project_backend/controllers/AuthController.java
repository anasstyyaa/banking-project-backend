package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.security.HmacCsrfTokenRepository;
import inholland.nl.banking_project_backend.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user identity verification, registration, and session management.")
public class AuthController {

    private final AuthService authService;
    private final HmacCsrfTokenRepository csrfTokenRepository;

    @Operation(summary = "Register a new customer account", description = "Submits registration credentials. The account will remain pending until approved by an employee.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account requested successfully.",
                    content = @Content(schema = @Schema(implementation = UserDTO.LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or duplicate email validation error.")
    })
    @PostMapping("/register")
    public ResponseEntity<UserDTO.LoginResponse> register(@Valid @RequestBody UserDTO.RegisterRequest dto) {
        return ResponseEntity.status(201).body(authService.register(dto));
    }

    @Operation(summary = "Authenticate credentials", description = "Validates account emails and passwords, returning a bearer token session key.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful.",
                    content = @Content(schema = @Schema(implementation = UserDTO.LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials supplied."),
            @ApiResponse(responseCode = "403", description = "Account registration exists but is currently unapproved.")
    })
    @PostMapping("/login")
    public ResponseEntity<UserDTO.LoginResponse> login(@Valid @RequestBody UserDTO.LoginRequest dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @Operation(
            summary = "Get current active user profile",
            description = "Resolves the bearer token inside the request context to return identity data for the currently logged-in user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Identity context resolved successfully."),
            @ApiResponse(responseCode = "401", description = "Missing or expired authorization bearer token.")
    })
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO.UserResponse> getMyProfile(@AuthenticationPrincipal UserModel currentUser) {
        UserDTO.UserResponse response = new UserDTO.UserResponse(
                currentUser.getFirstName(),
                currentUser.getLastName(),
                currentUser.getIban(),
                currentUser.getEmail(),
                currentUser.getRole()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(HttpServletRequest request) {
        String token = csrfTokenRepository.computeToken(request);
        return ResponseEntity.ok(Map.of("token", token != null ? token : ""));
    }

}
