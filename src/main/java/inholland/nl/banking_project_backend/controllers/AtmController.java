package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.AtmDTO;
import inholland.nl.banking_project_backend.services.AtmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/atm")
@RequiredArgsConstructor
public class AtmController {
    private final AtmService atmService;

    @GetMapping("/accounts")
    public ResponseEntity<List<AtmDTO.AccountResponse>> getAccounts(Principal principal) {
        return ResponseEntity.ok(atmService.getAccounts(principal.getName()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<AtmDTO.TransactionResponse> deposit(
            Principal principal, @Valid @RequestBody AtmDTO.MoneyRequest request
    ) {
        return ResponseEntity.ok(atmService.deposit(principal.getName(), request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<AtmDTO.TransactionResponse> withdraw(
            Principal principal, @Valid @RequestBody AtmDTO.MoneyRequest request
    ) {
        return ResponseEntity.ok(atmService.withdraw(principal.getName(), request));
    }
}
