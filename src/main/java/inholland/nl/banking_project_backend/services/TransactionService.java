package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.TransactionDTO;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.AbsoluteLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.DailyLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.InvalidTransactionException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.mappers.TransactionMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
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
    private final AccountService accountService;
    private final UserService userService;
    private final TransactionMapper transactionMapper;

    // Creates a transfer, deposit, or withdrawal for the authenticated user.
    @Transactional
    public TransactionDTO.TransactionResponse createTransaction(TransactionDTO.CreateRequest request, String userEmail) {
        validateAmount(request.amount());
        UserModel user = userService.findByEmail(userEmail);
        return switch (request.type()) {
            case TRANSFER -> handleTransfer(request, user);
            case DEPOSIT -> handleDeposit(request, user);
            case WITHDRAWAL -> handleWithdrawal(request, user);
        };
    }

    // Returns transactions visible to the authenticated user after applying filters.
    public List<TransactionDTO.TransactionResponse> getTransactions(TransactionDTO.FilterRequest filter, String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        return loadTransactions(filter).stream()
                .filter(transaction -> canViewTransaction(user, transaction))
                .filter(transaction -> matchesAmountFilters(transaction, filter))
                .filter(transaction -> matchesIbanFilter(transaction, filter.iban()))
                .map(transactionMapper::toResponse)
                .toList();
    }

    // Returns one transaction when the authenticated user may view it.
    public TransactionDTO.TransactionResponse getTransactionById(Long id, String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        TransactionModel transaction = findTransaction(id);
        validateCanViewTransaction(user, transaction);
        return transactionMapper.toResponse(transaction);
    }

    // Handles a transfer between two active bank accounts.
    private TransactionDTO.TransactionResponse handleTransfer(TransactionDTO.CreateRequest request, UserModel user) {
        validateTransferFields(request);
        AccountModel source = accountService.getActiveAccount(request.fromIban());
        AccountModel destination = accountService.getActiveAccount(request.toIban());
        validateDifferentAccounts(source, destination);
        validateOutgoingTransaction(user, source, request.amount());
        accountService.debit(source, request.amount());
        accountService.credit(destination, request.amount());
        return saveCompletedTransaction(request, source, destination, user);
    }

    // Handles an ATM deposit into an active account.
    private TransactionDTO.TransactionResponse handleDeposit(TransactionDTO.CreateRequest request, UserModel user) {
        validateDepositFields(request);
        AccountModel destination = accountService.getActiveAccount(request.toIban());
        accountService.validateCanUseAccount(user, destination);
        accountService.credit(destination, request.amount());
        return saveCompletedTransaction(request, null, destination, user);
    }

    // Handles an ATM withdrawal from an active account.
    private TransactionDTO.TransactionResponse handleWithdrawal(TransactionDTO.CreateRequest request, UserModel user) {
        validateWithdrawalFields(request);
        AccountModel source = accountService.getActiveAccount(request.fromIban());
        validateOutgoingTransaction(user, source, request.amount());
        accountService.debit(source, request.amount());
        return saveCompletedTransaction(request, source, null, user);
    }

    // Validates that the transaction amount is positive.
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be greater than zero.");
        }
    }

    // Validates required fields for transfer transactions.
    private void validateTransferFields(TransactionDTO.CreateRequest request) {
        if (isBlank(request.fromIban()) || isBlank(request.toIban())) {
            throw new InvalidTransactionException("Transfer requires both source and destination IBAN.");
        }
    }

    // Validates required fields for deposit transactions.
    private void validateDepositFields(TransactionDTO.CreateRequest request) {
        if (isBlank(request.toIban())) {
            throw new InvalidTransactionException("Deposit requires a destination IBAN.");
        }
    }

    // Validates required fields for withdrawal transactions.
    private void validateWithdrawalFields(TransactionDTO.CreateRequest request) {
        if (isBlank(request.fromIban())) {
            throw new InvalidTransactionException("Withdrawal requires a source IBAN.");
        }
    }

    // Prevents transferring money to the same account.
    private void validateDifferentAccounts(AccountModel source, AccountModel destination) {
        if (source.getIban().equals(destination.getIban())) {
            throw new InvalidTransactionException("Source and destination accounts must be different.");
        }
    }

    // Applies account access, absolute limit, and daily outgoing limit checks.
    private void validateOutgoingTransaction(UserModel user, AccountModel source, BigDecimal amount) {
        accountService.validateCanUseAccount(user, source);
        validateSourceAccountCanSend(user);
        validateAbsoluteLimit(source, amount);
        validateDailyLimit(source, amount);
    }

    // Ensures customers are approved before sending money.
    private void validateSourceAccountCanSend(UserModel user) {
        if (user.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return;
        }
        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new UnauthorizedAccountAccessException("Your account must be approved before making transactions.");
        }
    }

    // Rejects transactions that would move the balance below the account's absolute limit.
    private void validateAbsoluteLimit(AccountModel source, BigDecimal amount) {
        BigDecimal balanceAfterTransaction = source.getBalance().subtract(amount);
        if (balanceAfterTransaction.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new AbsoluteLimitExceededException("This transaction exceeds the account absolute limit.");
        }
    }

    // Rejects transactions that exceed the account's daily outgoing limit.
    private void validateDailyLimit(AccountModel source, BigDecimal amount) {
        BigDecimal dailyTotal = getDailyOutgoingTotal(source);
        if (dailyTotal.add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException("This transaction exceeds the account daily limit.");
        }
    }

    // Sums today's outgoing transfer and withdrawal amounts for an account.
    private BigDecimal getDailyOutgoingTotal(AccountModel source) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        return transactionRepository.findByFromIbanSnapshotAndTimestampBetween(source.getIban(), start, end)
                .stream()
                .filter(this::isOutgoingTransaction)
                .map(TransactionModel::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Returns whether the transaction counts against outgoing daily limits.
    private boolean isOutgoingTransaction(TransactionModel transaction) {
        return transaction.getType() == TransactionTypeEnum.TRANSFER
                || transaction.getType() == TransactionTypeEnum.WITHDRAWAL;
    }

    // Saves changed accounts and the final transaction record together.
    private TransactionDTO.TransactionResponse saveCompletedTransaction(
            TransactionDTO.CreateRequest request,
            AccountModel source,
            AccountModel destination,
            UserModel user
    ) {
        saveChangedAccount(source);
        saveChangedAccount(destination);
        TransactionModel transaction = transactionMapper.toModel(request, source, destination, user);
        TransactionModel savedTransaction = transactionRepository.save(transaction);
        return transactionMapper.toResponse(savedTransaction);
    }

    // Persists an account only when this transaction changed it.
    private void saveChangedAccount(AccountModel account) {
        if (account != null) {
            accountService.save(account);
        }
    }

    // Loads transactions inside the requested date range.
    private List<TransactionModel> loadTransactions(TransactionDTO.FilterRequest filter) {
        LocalDateTime start = getStartDateTime(filter.startDate());
        LocalDateTime end = getEndDateTime(filter.endDate());
        return transactionRepository.findByTimestampBetweenOrderByTimestampDesc(start, end);
    }

    // Converts an optional start date into a query boundary.
    private LocalDateTime getStartDateTime(LocalDate startDate) {
        return startDate == null ? LocalDateTime.of(1970, 1, 1, 0, 0) : startDate.atStartOfDay();
    }

    // Converts an optional end date into a query boundary.
    private LocalDateTime getEndDateTime(LocalDate endDate) {
        return endDate == null ? LocalDateTime.now().plusDays(1) : endDate.atTime(LocalTime.MAX);
    }

    // Checks whether the user may view a transaction.
    private boolean canViewTransaction(UserModel user, TransactionModel transaction) {
        if (user.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return true;
        }
        return ownsTransactionAccount(user, transaction.getFromAccount())
                || ownsTransactionAccount(user, transaction.getToAccount());
    }

    // Throws when the user may not view the transaction.
    private void validateCanViewTransaction(UserModel user, TransactionModel transaction) {
        if (!canViewTransaction(user, transaction)) {
            throw new UnauthorizedAccountAccessException("You are not allowed to view this transaction.");
        }
    }

    // Checks whether a transaction account belongs to the user.
    private boolean ownsTransactionAccount(UserModel user, AccountModel account) {
        return account != null && account.getCustomer().getUser().getEmail().equals(user.getEmail());
    }

    // Applies optional amount filters to a transaction.
    private boolean matchesAmountFilters(TransactionModel transaction, TransactionDTO.FilterRequest filter) {
        return matchesLessThan(transaction, filter.amountLessThan())
                && matchesGreaterThan(transaction, filter.amountGreaterThan())
                && matchesEqualTo(transaction, filter.amountEqualTo());
    }

    // Checks the optional less-than amount filter.
    private boolean matchesLessThan(TransactionModel transaction, BigDecimal amount) {
        return amount == null || transaction.getAmount().compareTo(amount) < 0;
    }

    // Checks the optional greater-than amount filter.
    private boolean matchesGreaterThan(TransactionModel transaction, BigDecimal amount) {
        return amount == null || transaction.getAmount().compareTo(amount) > 0;
    }

    // Checks the optional equal-to amount filter.
    private boolean matchesEqualTo(TransactionModel transaction, BigDecimal amount) {
        return amount == null || transaction.getAmount().compareTo(amount) == 0;
    }

    // Applies optional IBAN filtering against source or destination snapshots.
    private boolean matchesIbanFilter(TransactionModel transaction, String iban) {
        return isBlank(iban)
                || iban.equals(transaction.getFromIbanSnapshot())
                || iban.equals(transaction.getToIbanSnapshot());
    }

    // Loads a transaction by id or throws a clean not-found error.
    private TransactionModel findTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found."));
    }

    // Checks whether text is missing or only whitespace.
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
