package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.RegistrationDecisionEnum;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Users", description = "Endpoints for user profiles and employee management of customer registrations")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @Operation(summary = "Get own profile", description = "Returns the profile of the currently authenticated user.")
    @GetMapping("/profile")
    public ResponseEntity<UserDTO.ProfileResponse> getProfile(Principal principal) {
        return ResponseEntity.ok(userService.getProfile(principal.getName()));
    }

    @Operation(summary = "Update own profile", description = "Allows the authenticated user to update their profile information.")
    @PutMapping("/profile")
    public ResponseEntity<UserDTO.UpdateProfileResponse> updateProfile(
            @Valid @RequestBody UserDTO.UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    @Operation(summary = "Get customer registrations", description = "Returns a paginated list of customer registrations filtered by status (PENDING or APPROVED), with optional search.")
    @GetMapping("/registrations")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Page<UserDTO.RegistrationRequest>> getRegistrations(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserModel> users = "APPROVED".equalsIgnoreCase(status)
                ? userService.getActiveUsers(search, pageable)
                : userService.getPendingUsers(search, pageable);
        return ResponseEntity.ok(users.map(userMapper::toRegistrationRequest));
    }

    @Operation(summary = "Update registration status", description = "Allows an employee to approve (optionally setting initial absolute/daily account limits) or deny a pending registration.")
    @PatchMapping("/registrations/{id}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<Void> updateRegistrationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO.UpdateRegistrationStatusRequest request
    ) {
        if (request.status() == RegistrationDecisionEnum.APPROVED) {
            userService.approveUser(id, request);
        } else {
            userService.denyUser(id);
        }
        return ResponseEntity.noContent().build();
    }
}
