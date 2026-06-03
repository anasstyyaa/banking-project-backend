package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.TransactionDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.AbsoluteLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.DailyLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.InvalidTransactionException;
import inholland.nl.banking_project_backend.mappers.TransactionMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    // Creates a customer transfer, deposit, or withdrawal.
    @Transactional
    public TransactionDTO.TransactionResponse createCustomerTransaction(TransactionDTO.CreateRequest request, UserModel customer) {
        return switch (request.type()) {
            case TRANSFER -> createCustomerTransfer(request, customer);
            case DEPOSIT -> createCustomerDeposit(request, customer);
            case WITHDRAWAL -> createCustomerWithdrawal(request, customer);
        };
    }

    // Creates an employee transfer between customer checking accounts.
    @Transactional
    public TransactionDTO.TransactionResponse createEmployeeTransfer(TransactionDTO.CreateRequest request, UserModel employee) {
        if (request.type() != TransactionTypeEnum.TRANSFER) {
            throw new InvalidTransactionException("Employees can only create customer account transfers here.");
        }
        requireTransfer(request);

        AccountModel source = findActiveCheckingAccount(request.fromIban());
        AccountModel destination = findActiveCheckingAccount(request.toIban());
        validateDifferentAccounts(source, destination);
        validateOutgoingLimits(source, request.amount());
        moveMoney(source, destination, request.amount());
        return saveTransaction(request, source, destination, employee);
    }

    // Returns employee-visible transaction history using repository filters.
    public List<TransactionDTO.TransactionResponse> getTransactionsForEmployee(TransactionDTO.FilterRequest filter) {
        LocalDateTime start = getStartDateTime(filter.startDate());
        LocalDateTime end = getEndDateTime(filter.endDate());
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
    public List<TransactionDTO.TransactionResponse> getTransactionsForCustomer(TransactionDTO.FilterRequest filter, String customerEmail) {
        LocalDateTime start = getStartDateTime(filter.startDate());
        LocalDateTime end = getEndDateTime(filter.endDate());
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
    public TransactionDTO.TransactionResponse getTransactionForEmployee(Long id) {
        TransactionModel transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
        return transactionMapper.toResponse(transaction);
    }

    // Returns one customer-visible transaction by id.
    public TransactionDTO.TransactionResponse getTransactionForCustomer(Long id, String customerEmail) {
        TransactionModel transaction = transactionRepository.findCustomerVisibleTransactionById(id, customerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
        return transactionMapper.toResponse(transaction);
    }

    // Handles a customer transfer from one owned account to any active destination account.
    private TransactionDTO.TransactionResponse createCustomerTransfer(TransactionDTO.CreateRequest request, UserModel customer) {
        requireTransfer(request);
        AccountModel source = findActiveCustomerAccount(request.fromIban(), customer.getEmail());
        AccountModel destination = findActiveAccount(request.toIban());
        validateDifferentAccounts(source, destination);
        validateOutgoingLimits(source, request.amount());
        moveMoney(source, destination, request.amount());
        return saveTransaction(request, source, destination, customer);
    }

    // Handles a customer ATM deposit into an owned account.
    private TransactionDTO.TransactionResponse createCustomerDeposit(TransactionDTO.CreateRequest request, UserModel customer) {
        requireDeposit(request);
        AccountModel destination = findActiveCustomerAccount(request.toIban(), customer.getEmail());
        destination.setBalance(destination.getBalance().add(request.amount()));
        accountRepository.save(destination);
        return saveTransaction(request, null, destination, customer);
    }

    // Handles a customer ATM withdrawal from an owned account.
    private TransactionDTO.TransactionResponse createCustomerWithdrawal(TransactionDTO.CreateRequest request, UserModel customer) {
        requireWithdrawal(request);
        AccountModel source = findActiveCustomerAccount(request.fromIban(), customer.getEmail());
        validateOutgoingLimits(source, request.amount());
        source.setBalance(source.getBalance().subtract(request.amount()));
        accountRepository.save(source);
        return saveTransaction(request, source, null, customer);
    }

    // Requires the fields needed to transfer money.
    private void requireTransfer(TransactionDTO.CreateRequest request) {
        if (isBlank(request.fromIban()) || isBlank(request.toIban())) {
            throw new InvalidTransactionException("Transfer requires both source and destination IBAN.");
        }
    }

    // Requires the field needed to deposit money.
    private void requireDeposit(TransactionDTO.CreateRequest request) {
        if (isBlank(request.toIban())) {
            throw new InvalidTransactionException("Deposit requires a destination IBAN.");
        }
    }

    // Requires the field needed to withdraw money.
    private void requireWithdrawal(TransactionDTO.CreateRequest request) {
        if (isBlank(request.fromIban())) {
            throw new InvalidTransactionException("Withdrawal requires a source IBAN.");
        }
    }

    // Prevents transfers where source and destination are the same account.
    private void validateDifferentAccounts(AccountModel source, AccountModel destination) {
        if (source.getIban().equals(destination.getIban())) {
            throw new InvalidTransactionException("Source and destination accounts must be different.");
        }
    }

    // Validates balance and daily outgoing limits for outgoing transactions.
    private void validateOutgoingLimits(AccountModel source, BigDecimal amount) {
        BigDecimal balanceAfterTransaction = source.getBalance().subtract(amount);
        if (balanceAfterTransaction.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new AbsoluteLimitExceededException("This transaction exceeds the account absolute limit.");
        }

        BigDecimal dailyTotal = getDailyOutgoingTotal(source);
        if (dailyTotal.add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException("This transaction exceeds the account daily limit.");
        }
    }

    // Moves money between two managed account entities.
    private void moveMoney(AccountModel source, AccountModel destination, BigDecimal amount) {
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));
        accountRepository.save(source);
        accountRepository.save(destination);
    }

    // Stores the final transaction record after account balances are changed.
    private TransactionDTO.TransactionResponse saveTransaction(
            TransactionDTO.CreateRequest request,
            AccountModel source,
            AccountModel destination,
            UserModel initiatedBy
    ) {
        TransactionModel transaction = transactionMapper.toModel(request, source, destination, initiatedBy);
        return transactionMapper.toResponse(transactionRepository.save(transaction));
    }

    // Calculates today's outgoing transfer and withdrawal total for one account.
    private BigDecimal getDailyOutgoingTotal(AccountModel source) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        BigDecimal total = transactionRepository.sumOutgoingAmountForAccount(
                source.getIban(),
                start,
                end,
                List.of(TransactionTypeEnum.TRANSFER, TransactionTypeEnum.WITHDRAWAL)
        );
        return total == null ? BigDecimal.ZERO : total;
    }

    // Loads one active account by IBAN.
    private AccountModel findActiveAccount(String iban) {
        return accountRepository.findByIbanAndIsActiveTrue(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
    }

    // Loads one active checking account by IBAN.
    private AccountModel findActiveCheckingAccount(String iban) {
        return accountRepository.findByIbanAndTypeAndIsActiveTrue(iban, AccountTypeEnum.CHECKING)
                .orElseThrow(() -> new AccountNotFoundException("Active checking account not found."));
    }

    // Loads one active account owned by a customer.
    private AccountModel findActiveCustomerAccount(String iban, String email) {
        return accountRepository.findByIbanAndCustomerUserEmailAndIsActiveTrue(iban, email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
    }

    // Converts an optional start date into an inclusive query boundary.
    private LocalDateTime getStartDateTime(LocalDate startDate) {
        return startDate == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : startDate.atStartOfDay();
    }

    // Converts an optional end date into an inclusive query boundary.
    private LocalDateTime getEndDateTime(LocalDate endDate) {
        return endDate == null ? LocalDateTime.now().plusDays(1) : endDate.atTime(LocalTime.MAX);
    }

    // Normalizes blank query parameters before repository filtering.
    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    // Checks whether text is missing or only whitespace.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
