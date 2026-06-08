package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.LimitExceededException;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TransactionPolicy {
    private final TransactionRepository transactionRepository;

    // Validates a customer transfer before balances are changed.
    public void validateCustomerTransfer(CreateTransactionRequestDTO request, AccountModel source, AccountModel destination) {
        requireTransactionType(request, TransactionTypeEnum.TRANSFER);
        requireAccountField(source, "Transfer requires a source IBAN.");
        requireAccountField(destination, "Transfer requires a destination IBAN.");
        requireOpenAccount(source);
        requireOpenAccount(destination);
        requireDifferentAccounts(source, destination);
        requireExternalCustomerTransferAccounts(source, destination);
        requireOutgoingLimits(source, request.amount());
    }

    // Validates an employee transfer before balances are changed.
    public void validateEmployeeTransfer(CreateTransactionRequestDTO request, AccountModel source, AccountModel destination) {
        requireTransactionType(request, TransactionTypeEnum.TRANSFER);
        requireAccountField(source, "Transfer requires a source IBAN.");
        requireAccountField(destination, "Transfer requires a destination IBAN.");
        requireOpenAccount(source);
        requireOpenAccount(destination);
        requireCheckingTransfer(source, destination, "Employees can only transfer between checking accounts.");
        requireDifferentAccounts(source, destination);
        requireOutgoingLimits(source, request.amount());
    }

    // Validates an ATM deposit before the account balance is changed.
    public void validateDeposit(CreateTransactionRequestDTO request, AccountModel destination) {
        requireTransactionType(request, TransactionTypeEnum.DEPOSIT);
        requireAccountField(destination, "Deposit requires a destination IBAN.");
        requireOpenAccount(destination);
        requireCheckingAccount(destination, "ATM deposits can only be made into checking accounts.");
    }

    // Validates an ATM withdrawal before the account balance is changed.
    public void validateWithdrawal(CreateTransactionRequestDTO request, AccountModel source) {
        requireTransactionType(request, TransactionTypeEnum.WITHDRAWAL);
        requireAccountField(source, "Withdrawal requires a source IBAN.");
        requireOpenAccount(source);
        requireCheckingAccount(source, "ATM withdrawals can only be made from checking accounts.");
        requireOutgoingLimits(source, request.amount());
    }

    // Ensures a transaction request is being handled by the correct workflow.
    private void requireTransactionType(CreateTransactionRequestDTO request, TransactionTypeEnum expectedType) {
        if (request.type() != expectedType) {
            throw new IllegalArgumentException("Transaction type does not match this operation.");
        }
    }

    // Ensures a required transaction account field was provided and loaded.
    private void requireAccountField(AccountModel account, String message) {
        if (account == null) {
            throw new IllegalArgumentException(message);
        }
    }

    // Ensures a transaction account has not been closed.
    private void requireOpenAccount(AccountModel account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new IllegalStateException("This account is closed.");
        }
    }

    // Ensures transfers never target the same account.
    private void requireDifferentAccounts(AccountModel source, AccountModel destination) {
        if (source.getIban().equals(destination.getIban())) {
            throw new IllegalArgumentException("Source and destination accounts must be different.");
        }
    }

    // Ensures customer transfers to another customer only use checking accounts.
    private void requireExternalCustomerTransferAccounts(AccountModel source, AccountModel destination) {
        if (!accountsBelongToSameCustomer(source, destination)) {
            requireCheckingTransfer(source, destination, "External customer transfers can only be made between checking accounts.");
        }
    }

    // Ensures transfers that must use checking accounts do not involve savings accounts.
    private void requireCheckingTransfer(AccountModel source, AccountModel destination, String message) {
        if (source.getType() != AccountTypeEnum.CHECKING || destination.getType() != AccountTypeEnum.CHECKING) {
            throw new IllegalArgumentException(message);
        }
    }

    // Compares account ownership without requiring fully initialized entity equality.
    private boolean accountsBelongToSameCustomer(AccountModel source, AccountModel destination) {
        CustomerProfileModel sourceCustomer = source.getCustomer();
        CustomerProfileModel destinationCustomer = destination.getCustomer();

        if (sourceCustomer == null || destinationCustomer == null) {
            return false;
        }

        if (sourceCustomer.getId() != null && destinationCustomer.getId() != null) {
            return sourceCustomer.getId().equals(destinationCustomer.getId());
        }

        return sourceCustomer == destinationCustomer;
    }

    // Ensures ATM cash operations only use checking accounts.
    private void requireCheckingAccount(AccountModel account, String message) {
        if (account.getType() != AccountTypeEnum.CHECKING) {
            throw new IllegalArgumentException(message);
        }
    }

    // Ensures outgoing transactions respect the account absolute and daily limits.
    private void requireOutgoingLimits(AccountModel source, BigDecimal amount) {
        BigDecimal balanceAfterTransaction = source.getBalance().subtract(amount);
        if (balanceAfterTransaction.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new LimitExceededException("This transaction exceeds the account absolute limit.");
        }

        BigDecimal dailyTotal = getDailyOutgoingTotal(source);
        if (dailyTotal.add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new LimitExceededException("This transaction exceeds the account daily limit.");
        }
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
}
