package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.ApiErrorDTO;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Resource endpoints for bank accounts")
public class AccountController {
    private final AccountService accountService;

    // Returns accounts visible to the authenticated user.
    @Operation(summary = "Get visible accounts", description = "Returns own accounts for customers and all accounts for employees.")
    @GetMapping
    public ResponseEntity<?> getAccounts(Principal principal) {
        try {
            return ResponseEntity.ok(accountService.getAccountsForUser(principal.getName()));
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Accounts could not be loaded.");
        }
    }

    // Returns one account when the authenticated user may view it.
    @Operation(summary = "Get account by IBAN", description = "Returns one account if the authenticated user has access.")
    @GetMapping("/{iban}")
    public ResponseEntity<?> getAccount(@PathVariable String iban, Principal principal) {
        try {
            return ResponseEntity.ok(accountService.getAccountForUser(iban, principal.getName()));
        } catch (AccountNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (InactiveAccountException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (UnauthorizedAccountAccessException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Account could not be loaded.");
        }
    }

    // Builds a consistent account error response.
    private ResponseEntity<ApiErrorDTO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorDTO(message, LocalDateTime.now()));
    }
}
