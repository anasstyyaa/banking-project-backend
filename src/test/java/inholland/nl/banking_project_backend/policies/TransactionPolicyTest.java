package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.LimitExceededException;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionPolicyTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionPolicy transactionPolicy;
    private AccountModel source;
    private AccountModel destination;

    @BeforeEach
    void setUp() {
        transactionPolicy = new TransactionPolicy(transactionRepository);
        source = account("NL01INHO000000001", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        destination = account("NL01INHO000000002", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
    }

    // Valid customer transfers pass all transaction rules.
    @Test
    void validateCustomerTransfer_allowsValidTransfer() {
        whenDailyTotalIs("100.00");

        assertDoesNotThrow(() -> transactionPolicy.validateCustomerTransfer(transfer("200.00"), source, destination));
    }

    // Transfers require both a loaded source and destination account.
    @Test
    void validateCustomerTransfer_throwsWhenSourceIsMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), null, destination));
    }

    // Transfers cannot send money from an account to itself.
    @Test
    void validateCustomerTransfer_throwsWhenAccountsAreTheSame() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), source, source));
    }

    // Employee transfers must move money between checking accounts.
    @Test
    void validateEmployeeTransfer_throwsForNonCheckingDestination() {
        destination.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateEmployeeTransfer(transfer("100.00"), source, destination));
    }

    // Outgoing transactions cannot move the account below the absolute limit.
    @Test
    void validateWithdrawal_throwsWhenAbsoluteLimitWouldBeExceeded() {
        assertThrows(LimitExceededException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("1600.00"), source));
    }

    // Outgoing transactions cannot exceed today's daily transfer limit.
    @Test
    void validateWithdrawal_throwsWhenDailyLimitWouldBeExceeded() {
        whenDailyTotalIs("900.00");

        assertThrows(LimitExceededException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("200.00"), source));
    }

    // Deposits only require an open destination account and do not check outgoing limits.
    @Test
    void validateDeposit_allowsOpenDestinationAccount() {
        assertDoesNotThrow(() -> transactionPolicy.validateDeposit(deposit("100.00"), destination));
    }

    // ATM deposits are only allowed into checking accounts.
    @Test
    void validateDeposit_throwsForSavingsDestination() {
        destination.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateDeposit(deposit("100.00"), destination));
    }

    // ATM withdrawals are only allowed from checking accounts.
    @Test
    void validateWithdrawal_throwsForSavingsSource() {
        source.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("100.00"), source));
    }

    // Closed accounts cannot be used in transaction workflows.
    @Test
    void validateDeposit_throwsForClosedDestinationAccount() {
        destination.setIsActive(false);

        assertThrows(IllegalStateException.class,
                () -> transactionPolicy.validateDeposit(deposit("100.00"), destination));
    }

    private void whenDailyTotalIs(String amount) {
        when(transactionRepository.sumOutgoingAmountForAccount(eq(source.getIban()), any(), any(), any()))
                .thenReturn(new BigDecimal(amount));
    }

    private CreateTransactionRequestDTO transfer(String amount) {
        return new CreateTransactionRequestDTO(TransactionTypeEnum.TRANSFER, source.getIban(), destination.getIban(), new BigDecimal(amount));
    }

    private CreateTransactionRequestDTO deposit(String amount) {
        return new CreateTransactionRequestDTO(TransactionTypeEnum.DEPOSIT, null, destination.getIban(), new BigDecimal(amount));
    }

    private CreateTransactionRequestDTO withdrawal(String amount) {
        return new CreateTransactionRequestDTO(TransactionTypeEnum.WITHDRAWAL, source.getIban(), null, new BigDecimal(amount));
    }

    private AccountModel account(String iban, AccountTypeEnum type, String balance, String absoluteLimit, String dailyLimit) {
        AccountModel account = new AccountModel();
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal(balance));
        account.setAbsoluteLimit(new BigDecimal(absoluteLimit));
        account.setDailyLimit(new BigDecimal(dailyLimit));
        account.setIsActive(true);
        return account;
    }
}
