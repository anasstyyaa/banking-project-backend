package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionFilterRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionResponseDTO;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.InvalidTransactionException;
import inholland.nl.banking_project_backend.mappers.TransactionMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.policies.AccountPolicy;
import inholland.nl.banking_project_backend.policies.TransactionPolicy;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;
    private final AccountPolicy accountPolicy;
    private final TransactionPolicy transactionPolicy;

    // Creates a customer transfer, deposit, or withdrawal.
    @Transactional
    public TransactionResponseDTO createCustomerTransaction(CreateTransactionRequestDTO request, UserModel customer) {
        return switch (request.type()) {
            case TRANSFER -> createCustomerTransfer(request, customer);
            case DEPOSIT -> createCustomerDeposit(request, customer);
            case WITHDRAWAL -> createCustomerWithdrawal(request, customer);
        };
    }

    // Creates an employee transfer between customer checking accounts.
    @Transactional
    public TransactionResponseDTO createEmployeeTransfer(CreateTransactionRequestDTO request, UserModel employee) {
        if (request.type() != TransactionTypeEnum.TRANSFER) {
            throw new InvalidTransactionException("Employees can only create customer account transfers here.");
        }

        AccountModel source = accountRepository.findByIban(request.fromIban())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found."));
        AccountModel destination = accountRepository.findByIban(request.toIban())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found."));

        accountPolicy.requireOpenAccount(source);
        accountPolicy.requireOpenAccount(destination);
        transactionPolicy.requireCheckingTransfer(source, destination);
        transactionPolicy.requireDifferentAccounts(source, destination);
        transactionPolicy.requireOutgoingLimits(source, request.amount());
        transferFunds(source, destination, request.amount());
        return saveTransaction(request, source, destination, employee);
    }

    // Returns employee-visible transaction history.
    public List<TransactionResponseDTO> getTransactionsForEmployee(TransactionFilterRequestDTO filter) {
        LocalDateTime start = filter.startDate() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : filter.startDate().atStartOfDay();
        LocalDateTime end = filter.endDate() == null ? LocalDateTime.now().plusDays(1) : filter.endDate().atTime(LocalTime.MAX);
        return transactionRepository.findEmployeeVisibleTransactions(
                        start,
                        end,
                        filter.amountLessThan(),
                        filter.amountGreaterThan(),
                        filter.amountEqualTo(),
                        blankToNull(filter.iban()),
                        filter.userId()
                )
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Returns customer-visible transaction history using repository filters.
    public List<TransactionResponseDTO> getTransactionsForCustomer(TransactionFilterRequestDTO filter, String customerEmail) {
        LocalDateTime start = filter.startDate() == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : filter.startDate().atStartOfDay();
        LocalDateTime end = filter.endDate() == null ? LocalDateTime.now().plusDays(1) : filter.endDate().atTime(LocalTime.MAX);
        return transactionRepository.findCustomerVisibleTransactions(
                        customerEmail,
                        start,
                        end,
                        filter.amountLessThan(),
                        filter.amountGreaterThan(),
                        filter.amountEqualTo(),
                        blankToNull(filter.iban())
                )
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Returns one employee-visible transaction by id.
    public TransactionResponseDTO getTransactionForEmployee(Long id) {
        TransactionModel transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
        return transactionMapper.toResponse(transaction);
    }

    // Returns one customer-visible transaction by id.
    public TransactionResponseDTO getTransactionForCustomer(Long id, String customerEmail) {
        TransactionModel transaction = transactionRepository.findCustomerVisibleTransactionById(id, customerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
        return transactionMapper.toResponse(transaction);
    }

    // Handles a customer transfer from one owned account to any open destination account.
    private TransactionResponseDTO createCustomerTransfer(CreateTransactionRequestDTO request, UserModel customer) {
        AccountModel source = accountRepository.findByIbanAndCustomerUserEmail(request.fromIban(), customer.getEmail())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found."));
        AccountModel destination = accountRepository.findByIban(request.toIban())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found."));

        accountPolicy.requireOpenAccount(source);
        accountPolicy.requireOpenAccount(destination);
        transactionPolicy.requireDifferentAccounts(source, destination);
        transactionPolicy.requireOutgoingLimits(source, request.amount());
        transferFunds(source, destination, request.amount());
        return saveTransaction(request, source, destination, customer);
    }

    // Handles a customer ATM deposit into an owned account.
    private TransactionResponseDTO createCustomerDeposit(CreateTransactionRequestDTO request, UserModel customer) {
        AccountModel destination = accountRepository.findByIbanAndCustomerUserEmail(request.toIban(), customer.getEmail())
                .orElseThrow(() -> new AccountNotFoundException("Destination account not found."));

        accountPolicy.requireOpenAccount(destination);
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(destination);
        return saveTransaction(request, null, destination, customer);
    }

    // Handles a customer ATM withdrawal from an owned account.
    private TransactionResponseDTO createCustomerWithdrawal(CreateTransactionRequestDTO request, UserModel customer) {
        AccountModel source = accountRepository.findByIbanAndCustomerUserEmail(request.fromIban(), customer.getEmail())
                .orElseThrow(() -> new AccountNotFoundException("Source account not found."));

        accountPolicy.requireOpenAccount(source);
        transactionPolicy.requireOutgoingLimits(source, request.amount());
        source.setBalance(source.getBalance().subtract(request.amount()));
        accountRepository.save(source);
        return saveTransaction(request, source, null, customer);
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
