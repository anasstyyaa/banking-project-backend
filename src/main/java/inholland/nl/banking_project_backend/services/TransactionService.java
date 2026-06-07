package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionFilterRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionResponseDTO;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.mappers.TransactionMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.policies.TransactionPolicy;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionPolicy transactionPolicy;

    // Creates a transaction using an optional owner scope from the controller.
    @Transactional
    public TransactionResponseDTO createTransaction(CreateTransactionRequestDTO request, UserModel initiatedBy, String ownerEmail) {
        if (ownerEmail == null && request.type() != TransactionTypeEnum.TRANSFER) {
            throw new IllegalArgumentException("Only transfers can be created for customer accounts by staff.");
        }

        return switch (request.type()) {
            case TRANSFER -> createTransfer(request, initiatedBy, ownerEmail);
            case DEPOSIT -> createDeposit(request, initiatedBy, ownerEmail);
            case WITHDRAWAL -> createWithdrawal(request, initiatedBy, ownerEmail);
        };
    }

    // Returns transactions using repository filters and an optional viewer scope.
    public Page<TransactionResponseDTO> getTransactions(TransactionFilterRequestDTO filter, String viewerEmail, Pageable pageable) {
        LocalDateTime start = filter.startDate() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : filter.startDate().atStartOfDay();
        LocalDateTime end = filter.endDate() == null ? LocalDateTime.now().plusDays(1) : filter.endDate().atTime(LocalTime.MAX);
        return transactionRepository.findTransactions(
                        start,
                        end,
                        viewerEmail,
                        filter.amountLessThan(),
                        filter.amountGreaterThan(),
                        filter.amountEqualTo(),
                        blankToNull(filter.iban()),
                        filter.userId(),
                        pageable
                )
                .map(transactionMapper::toResponse);
    }

    // Returns one transaction using an optional viewer scope.
    public TransactionResponseDTO getTransaction(Long id, String viewerEmail) {
        TransactionModel transaction = transactionRepository.findTransactionById(id, viewerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
        return transactionMapper.toResponse(transaction);
    }

    // Handles a transfer using scoped account lookup when the caller is a customer.
    private TransactionResponseDTO createTransfer(CreateTransactionRequestDTO request, UserModel initiatedBy, String ownerEmail) {
        AccountModel source = findAccount(request.fromIban(), ownerEmail, "Source account not found.");
        AccountModel destination = findAccount(request.toIban(), null, "Destination account not found.");

        if (ownerEmail == null) {
            transactionPolicy.validateEmployeeTransfer(request, source, destination);
        } else {
            transactionPolicy.validateCustomerTransfer(request, source, destination);
        }

        transferFunds(source, destination, request.amount());
        return saveTransaction(request, source, destination, initiatedBy);
    }

    // Handles an ATM deposit into an account within the caller scope.
    private TransactionResponseDTO createDeposit(CreateTransactionRequestDTO request, UserModel initiatedBy, String ownerEmail) {
        AccountModel destination = findAccount(request.toIban(), ownerEmail, "Destination account not found.");

        transactionPolicy.validateDeposit(request, destination);
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(destination);
        return saveTransaction(request, null, destination, initiatedBy);
    }

    // Handles an ATM withdrawal from an account within the caller scope.
    private TransactionResponseDTO createWithdrawal(CreateTransactionRequestDTO request, UserModel initiatedBy, String ownerEmail) {
        AccountModel source = findAccount(request.fromIban(), ownerEmail, "Source account not found.");

        transactionPolicy.validateWithdrawal(request, source);
        source.setBalance(source.getBalance().subtract(request.amount()));
        accountRepository.save(source);
        return saveTransaction(request, source, null, initiatedBy);
    }

    // Finds an account when an IBAN was provided, otherwise lets policy report the missing field.
    private AccountModel findAccount(String iban, String ownerEmail, String notFoundMessage) {
        if (iban == null || iban.isBlank()) {
            return null;
        }

        return accountRepository.findAccountByIban(iban, ownerEmail)
                .orElseThrow(() -> new EntityNotFoundException(notFoundMessage));
    }

    // Moves money between two managed account entities.
    private void transferFunds(AccountModel source, AccountModel destination, BigDecimal amount) {
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);
    }

    // Stores the final transaction record after account balances are changed.
    private TransactionResponseDTO saveTransaction(
            CreateTransactionRequestDTO request,
            AccountModel source,
            AccountModel destination,
            UserModel initiatedBy
    ) {
        TransactionModel transaction = transactionMapper.toModel(request, source, destination, initiatedBy);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    // Normalizes blank query parameters before repository filtering.
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
