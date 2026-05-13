package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.ApiErrorDTO;
import inholland.nl.banking_project_backend.dtos.TransactionDTO;
import inholland.nl.banking_project_backend.exceptions.AbsoluteLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.DailyLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.exceptions.InvalidTransactionException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Resource endpoints for transfers, ATM deposits, and ATM withdrawals")
public class TransactionController {
    private final TransactionService transactionService;

    // Returns filtered transactions visible to the authenticated user.
    @Operation(summary = "Get transactions", description = "Returns transaction history with optional date, amount, and IBAN filters.")
    @GetMapping
    public ResponseEntity<?> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal amountLessThan,
            @RequestParam(required = false) BigDecimal amountGreaterThan,
            @RequestParam(required = false) BigDecimal amountEqualTo,
            @RequestParam(required = false) String iban,
            Principal principal
    ) {
        try {
            TransactionDTO.FilterRequest filter = new TransactionDTO.FilterRequest(
                    startDate, endDate, amountLessThan, amountGreaterThan, amountEqualTo, iban
            );
            return ResponseEntity.ok(transactionService.getTransactions(filter, principal.getName()));
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Transactions could not be loaded.");
        }
    }

    // Returns one transaction visible to the authenticated user.
    @Operation(summary = "Get transaction by id", description = "Returns one transaction when the user has access.")
    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id, Principal principal) {
        try {
            return ResponseEntity.ok(transactionService.getTransactionById(id, principal.getName()));
        } catch (EntityNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (UnauthorizedAccountAccessException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction could not be loaded.");
        }
    }

    // Creates a transfer, deposit, or withdrawal.
    @Operation(summary = "Create transaction", description = "Creates a transfer, ATM deposit, or ATM withdrawal using a transaction type.")
    @PostMapping
    public ResponseEntity<?> createTransaction(@Valid @RequestBody TransactionDTO.CreateRequest request, Principal principal) {
        try {
            return ResponseEntity.status(201).body(transactionService.createTransaction(request, principal.getName()));
        } catch (InvalidTransactionException exception) {
            return error(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (AccountNotFoundException exception) {
            return error(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (InactiveAccountException exception) {
            return error(HttpStatus.CONFLICT, exception.getMessage());
        } catch (UnauthorizedAccountAccessException exception) {
            return error(HttpStatus.FORBIDDEN, exception.getMessage());
        } catch (AbsoluteLimitExceededException | DailyLimitExceededException exception) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        } catch (Exception exception) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Transaction could not be completed.");
        }
    }

    // Builds a consistent transaction error response.
    private ResponseEntity<ApiErrorDTO> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorDTO(message, LocalDateTime.now()));
    }
}
