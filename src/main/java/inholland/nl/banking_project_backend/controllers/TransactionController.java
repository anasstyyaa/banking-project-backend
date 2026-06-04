package inholland.nl.banking_project_backend.controllers;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionFilterRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionResponseDTO;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Resource endpoints for transfers, ATM deposits, and ATM withdrawals")
public class TransactionController {
    private final TransactionService transactionService;

    // Returns filtered transactions visible to the authenticated user.
    @Operation(summary = "Get transactions", description = "Returns transaction history with optional date, amount, IBAN, and customer filters.")
    @GetMapping
    public List<TransactionResponseDTO> getTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal amountLessThan,
            @RequestParam(required = false) BigDecimal amountGreaterThan,
            @RequestParam(required = false) BigDecimal amountEqualTo,
            @RequestParam(required = false) String iban,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserModel currentUser
    ) {
        TransactionFilterRequestDTO filter = new TransactionFilterRequestDTO(
                startDate, endDate, amountLessThan, amountGreaterThan, amountEqualTo, iban, userId
        );

        if (currentUser.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return transactionService.getTransactionsForEmployee(filter);
        }
        return transactionService.getTransactionsForCustomer(filter, currentUser.getEmail());
    }

    // Returns one transaction visible to the authenticated user.
    @Operation(summary = "Get transaction by id", description = "Returns one transaction when the user has access.")
    @GetMapping("/{id}")
    public TransactionResponseDTO getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserModel currentUser
    ) {
        if (currentUser.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return transactionService.getTransactionForEmployee(id);
        }
        return transactionService.getTransactionForCustomer(id, currentUser.getEmail());
    }

    // Creates a transaction for customers or an employee checking-account transfer.
    @Operation(summary = "Create transaction", description = "Creates a transfer, ATM deposit, or ATM withdrawal using a transaction type.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponseDTO createTransaction(
            @Valid @RequestBody CreateTransactionRequestDTO request,
            @AuthenticationPrincipal UserModel currentUser
    ) {
        if (currentUser.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return transactionService.createEmployeeTransfer(request, currentUser);
        }
        return transactionService.createCustomerTransaction(request, currentUser);
    }
}
