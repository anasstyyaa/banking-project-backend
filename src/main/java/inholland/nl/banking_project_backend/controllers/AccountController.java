package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.services.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Resource endpoints for bank accounts")
public class AccountController {
    private final AccountService accountService;

    // Returns accounts visible to the authenticated user.
    @Operation(summary = "Get visible accounts", description = "Returns own accounts for customers and all accounts for employees.")
    @GetMapping
    public List<AccountDTO.AccountResponse> getAccounts(Principal principal) {
        return accountService.getAccountsForUser(principal.getName());
    }

    // Returns one account when the authenticated user may view it.
    @Operation(summary = "Get account by IBAN", description = "Returns one account if the authenticated user has access.")
    @GetMapping("/{iban}")
    public AccountDTO.AccountResponse getAccount(@PathVariable String iban, Principal principal) {
        return accountService.getAccountForUser(iban, principal.getName());
    }
}
