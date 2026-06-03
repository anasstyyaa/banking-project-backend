package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.AccountService;
import inholland.nl.banking_project_backend.services.UserService;
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

    @GetMapping("/pending")
    public ResponseEntity<List<UserDTO.RegistrationRequest>> getPendingRegistrations() {
        return ResponseEntity.ok(
                userService.getPendingUsers()
                        .stream()
                        .map(userMapper::toRegistrationRequest)
                        .toList()
        );
    }

    @GetMapping("/active-customers")
    public ResponseEntity<List<UserModel>> getActiveCustomers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }

    @PostMapping("/approve/{id}")
    public ResponseEntity<Void> approveUser(@PathVariable Long id) {
        userService.approveUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deny/{id}")
    public ResponseEntity<Void> denyUser(@PathVariable Long id) {
        userService.denyUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/customers/{userId}/accounts")
    public ResponseEntity<AccountDTO.AccountResponse> createCustomerAccount(
            @PathVariable Long userId,
            @RequestBody AccountDTO.AccountCreationRequest request) {

        AccountTypeEnum type = AccountTypeEnum.valueOf(request.accountType().toUpperCase());
        return ResponseEntity.ok(accountService.createAdditionalAccount(userId, type));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<UserModel>> getActiveCustomers() {
        return ResponseEntity.ok(userService.getActiveUsers());
    }
    
}