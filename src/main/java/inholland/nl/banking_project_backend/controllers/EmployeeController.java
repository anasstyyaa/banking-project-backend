package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.AccountService;
import inholland.nl.banking_project_backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
public class EmployeeController {
    private final AccountService accountService;
    private final UserService userService;
    private final UserMapper userMapper;

    // Returns customers waiting for employee approval.
    @GetMapping("/pending")
    public ResponseEntity<List<UserDTO.RegistrationRequest>> getPendingRegistrations() {
        return ResponseEntity.ok(
                userService.getPendingUsers()
                        .stream()
                        .map(userMapper::toRegistrationRequest)
                        .toList()
        );
    }

    // Approves one pending customer registration.
    @PostMapping("/approve/{id}")
    public ResponseEntity<Void> approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.noContent().build();
    }

    // Denies one pending customer registration.
    @DeleteMapping("/deny/{id}")
    public ResponseEntity<Void> denyUser(@PathVariable Long id) {
        userService.denyUser(id);
        return ResponseEntity.noContent().build();
    }

    // Creates a new customer account with employee-defined limits.
    @PostMapping("/customers/{userId}/accounts")
    public ResponseEntity<AccountDTO.AccountResponse> createCustomerAccount(
            @PathVariable Long userId,
            @Valid @RequestBody AccountDTO.AccountCreationRequest request) {
        return ResponseEntity.ok(accountService.createAdditionalAccount(userId, request));
    }

    // Returns approved customers for employee workflows.
    @GetMapping("/customers")
    public ResponseEntity<List<UserModel>> getActiveCustomers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }
    
}
