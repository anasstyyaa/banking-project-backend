package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.AccountResponseDTO;
import inholland.nl.banking_project_backend.dtos.AccountSearchResponseDTO;
import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Resource endpoints for bank accounts")
public class AccountController {
    private final AccountService accountService;

    // Returns active accounts visible to the authenticated user.
    @Operation(summary = "Get visible accounts", description = "Returns own accounts for customers and all active accounts for employees.")
    @GetMapping
    public ResponseEntity<Page<AccountResponseDTO>> getAccounts(
            @AuthenticationPrincipal UserModel currentUser,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        String ownerEmail = currentUser.getRole() == RoleEnum.ROLE_EMPLOYEE ? null : currentUser.getEmail();
        return ResponseEntity.ok(accountService.getAccounts(ownerEmail, search, pageable));
    }

    // Returns one active account visible to the authenticated user.
    @Operation(summary = "Get account by IBAN", description = "Returns one account if the authenticated user has access.")
    @GetMapping("/{iban}")
    public ResponseEntity<AccountResponseDTO> getAccount(
            @PathVariable String iban,
            @AuthenticationPrincipal UserModel currentUser
    ) {
        String ownerEmail = currentUser.getRole() == RoleEnum.ROLE_EMPLOYEE ? null : currentUser.getEmail();
        return ResponseEntity.ok(accountService.getAccount(iban, ownerEmail));
    }

    // Searches active accounts with a lightweight IBAN lookup response.
    @Operation(summary = "Search accounts", description = "Searches active accounts by customer name or IBAN.")
    @GetMapping("/search")
    public ResponseEntity<Page<AccountSearchResponseDTO>> searchAccounts(
            @RequestParam String query,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.searchAccounts(query, pageable));
    }

    // Creates a new customer account with employee-defined limits.
    @Operation(summary = "Create account", description = "Creates a new customer account with employee-defined limits.")
    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> createAccount(@Valid @RequestBody CreateAccountRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    // Closes an active account with a zero balance.
    @Operation(summary = "Close account", description = "Allows an employee to close an account that has a zero balance.")
    @PatchMapping("/{iban}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> closeAccount(@PathVariable String iban) {
        return ResponseEntity.ok(accountService.closeAccount(iban));
    }

    // Updates absolute and daily transfer limits for an active account.
    @Operation(summary = "Update account limits", description = "Allows an employee to update absolute and daily transfer limits.")
    @PatchMapping("/{iban}/limits")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<AccountResponseDTO> updateLimits(
            @PathVariable String iban,
            @Valid @RequestBody UpdateAccountLimitsRequestDTO request
    ) {
        return ResponseEntity.ok(accountService.updateLimits(iban, request));
    }
}
