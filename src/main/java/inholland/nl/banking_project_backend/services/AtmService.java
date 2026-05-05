package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AtmDTO;
import inholland.nl.banking_project_backend.models.*;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AtmService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public List<AtmDTO.AccountResponse> getAccounts(String email) {
        return accountRepository.findByUserEmail(email).stream()
                .map(this::toAccountResponse)
                .toList();
    }

    public AtmDTO.TransactionResponse deposit(String email, AtmDTO.MoneyRequest request) {
        AccountModel account = findAccount(email, request.accountId());
        increaseBalance(account, request.amount());
        accountRepository.save(account);
        return saveTransaction(account, null, account, request.amount(), TransactionTypeEnum.ATM_DEPOSIT);
    }

    public AtmDTO.TransactionResponse withdraw(String email, AtmDTO.MoneyRequest request) {
        AccountModel account = findAccount(email, request.accountId());
        validateWithdrawal(account, request.amount());
        decreaseBalance(account, request.amount());
        accountRepository.save(account);
        return saveTransaction(account, account, null, request.amount(), TransactionTypeEnum.ATM_WITHDRAWAL);
    }

    private AccountModel findAccount(String email, Long accountId) {
        return accountRepository.findByIdAndUserEmail(accountId, email)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private void increaseBalance(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

    private void decreaseBalance(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
    }

    private void validateWithdrawal(AccountModel account, BigDecimal amount) {
        ensureWithinBalance(account, amount);
        ensureWithinAbsoluteLimit(account, amount);
        ensureWithinDailyLimit(account, amount);
    }

    private void ensureWithinBalance(AccountModel account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private void ensureWithinAbsoluteLimit(AccountModel account, BigDecimal amount) {
        if (projectedBalance(account, amount).compareTo(account.getAbsoluteLimit()) < 0) {
            throw new IllegalArgumentException("Withdrawal exceeds absolute limit");
        }
    }

    private void ensureWithinDailyLimit(AccountModel account, BigDecimal amount) {
        if (withdrawnToday(account).add(amount).compareTo(account.getDailyLimit()) > 0) {
            throw new IllegalArgumentException("Withdrawal exceeds daily limit");
        }
    }

    private BigDecimal projectedBalance(AccountModel account, BigDecimal amount) {
        return account.getBalance().subtract(amount);
    }

    private BigDecimal withdrawnToday(AccountModel account) {
        return transactionRepository.sumAmountByAccountTypeAndPeriod(
                account, TransactionTypeEnum.ATM_WITHDRAWAL, startOfDay(), endOfDay()
        );
    }

    private Instant startOfDay() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant endOfDay() {
        return LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private AtmDTO.TransactionResponse saveTransaction(
            AccountModel account, AccountModel fromAccount, AccountModel toAccount,
            BigDecimal amount, TransactionTypeEnum type
    ) {
        TransactionModel transaction = createTransaction(account, fromAccount, toAccount, amount, type);
        return toTransactionResponse(transactionRepository.save(transaction), account);
    }

    private TransactionModel createTransaction(
            AccountModel account, AccountModel fromAccount, AccountModel toAccount,
            BigDecimal amount, TransactionTypeEnum type
    ) {
        TransactionModel transaction = new TransactionModel();
        fillTransaction(transaction, account, fromAccount, toAccount, amount, type);
        return transaction;
    }

    private void fillTransaction(
            TransactionModel transaction, AccountModel account, AccountModel fromAccount,
            AccountModel toAccount, BigDecimal amount, TransactionTypeEnum type
    ) {
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setTimestamp(Instant.now());
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setInitiatedBy(account.getUser());
    }

    private AtmDTO.AccountResponse toAccountResponse(AccountModel account) {
        return new AtmDTO.AccountResponse(
                account.getId(), account.getIban(), account.getType(),
                account.getBalance(), account.getAbsoluteLimit(), account.getDailyLimit()
        );
    }

    private AtmDTO.TransactionResponse toTransactionResponse(TransactionModel transaction, AccountModel account) {
        return new AtmDTO.TransactionResponse(
                transaction.getId(), account.getId(), account.getIban(),
                transaction.getType(), transaction.getAmount(),
                account.getBalance(), transaction.getTimestamp()
        );
    }
}
