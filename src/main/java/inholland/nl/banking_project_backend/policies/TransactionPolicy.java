package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.AbsoluteLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.DailyLimitExceededException;
import inholland.nl.banking_project_backend.exceptions.InvalidTransactionException;
import inholland.nl.banking_project_backend.models.AccountModel;
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

    // Ensures transfers never target the same account.
    public void requireDifferentAccounts(AccountModel source, AccountModel destination) {
        if (source.getIban().equals(destination.getIban())) {
            throw new InvalidTransactionException("Source and destination accounts must be different.");
        }
    }

    // Ensures employee transfers only move money between customer checking accounts.
    public void requireCheckingTransfer(AccountModel source, AccountModel destination) {
        if (source.getType() != AccountTypeEnum.CHECKING || destination.getType() != AccountTypeEnum.CHECKING) {
            throw new InvalidTransactionException("Employees can only transfer between checking accounts.");
        }
    }

    // Ensures outgoing transactions respect the account absolute and daily limits.
    public void requireOutgoingLimits(AccountModel source, BigDecimal amount) {
        BigDecimal balanceAfterTransaction = source.getBalance().subtract(amount);
        if (balanceAfterTransaction.compareTo(source.getAbsoluteLimit()) < 0) {
            throw new AbsoluteLimitExceededException("This transaction exceeds the account absolute limit.");
        }

        BigDecimal dailyTotal = getDailyOutgoingTotal(source);
        if (dailyTotal.add(amount).compareTo(source.getDailyLimit()) > 0) {
            throw new DailyLimitExceededException("This transaction exceeds the account daily limit.");
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
