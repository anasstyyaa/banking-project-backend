package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.exceptions.LimitExceededException;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionPolicyTest {

    private TransactionPolicy transactionPolicy;
    private AccountModel source;
    private AccountModel destination;

    @BeforeEach
    void setUp() {
        transactionPolicy = new TransactionPolicy();
        source = account("NL01INHO000000001", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        destination = account("NL01INHO000000002", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
    }

    @Test
    void validateCustomerTransfer_allowsValidTransfer() {
        assertDoesNotThrow(() -> transactionPolicy.validateCustomerTransfer(transfer("200.00"), source, destination, dailyTotal("100.00")));
    }

    @Test
    void validateCustomerTransfer_throwsWhenSourceIsMissing() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), null, destination, BigDecimal.ZERO));
    }

    @Test
    void validateCustomerTransfer_throwsWhenAccountsAreTheSame() {
        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), source, source, BigDecimal.ZERO));
    }

    @Test
    void validateCustomerTransfer_allowsOwnSavingsToCheckingTransfer() {
        CustomerProfileModel owner = customer(1L);
        source.setCustomer(owner);
        destination.setCustomer(owner);
        source.setType(AccountTypeEnum.SAVINGS);

        assertDoesNotThrow(() -> transactionPolicy.validateCustomerTransfer(transfer("200.00"), source, destination, dailyTotal("100.00")));
    }

    @Test
    void validateCustomerTransfer_throwsForExternalSavingsSource() {
        source.setCustomer(customer(1L));
        destination.setCustomer(customer(2L));
        source.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), source, destination, BigDecimal.ZERO));
    }

    @Test
    void validateCustomerTransfer_throwsForExternalSavingsDestination() {
        source.setCustomer(customer(1L));
        destination.setCustomer(customer(2L));
        destination.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateCustomerTransfer(transfer("100.00"), source, destination, BigDecimal.ZERO));
    }

    @Test
    void validateEmployeeTransfer_throwsForNonCheckingDestination() {
        destination.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateEmployeeTransfer(transfer("100.00"), source, destination, BigDecimal.ZERO));
    }

    @Test
    void validateWithdrawal_throwsWhenAbsoluteLimitWouldBeExceeded() {
        assertThrows(LimitExceededException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("1600.00"), source, BigDecimal.ZERO));
    }

    @Test
    void validateWithdrawal_throwsWhenDailyLimitWouldBeExceeded() {
        assertThrows(LimitExceededException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("200.00"), source, dailyTotal("900.00")));
    }

    @Test
    void validateDeposit_allowsOpenDestinationAccount() {
        assertDoesNotThrow(() -> transactionPolicy.validateDeposit(deposit("100.00"), destination, dailyTotal("100.00")));
    }

    @Test
    void validateDeposit_throwsWhenDailyLimitWouldBeExceeded() {
        assertThrows(LimitExceededException.class,
                () -> transactionPolicy.validateDeposit(deposit("200.00"), destination, dailyTotal("900.00")));
    }

    @Test
    void validateDeposit_throwsForSavingsDestination() {
        destination.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateDeposit(deposit("100.00"), destination, BigDecimal.ZERO));
    }

    @Test
    void validateWithdrawal_throwsForSavingsSource() {
        source.setType(AccountTypeEnum.SAVINGS);

        assertThrows(IllegalArgumentException.class,
                () -> transactionPolicy.validateWithdrawal(withdrawal("100.00"), source, BigDecimal.ZERO));
    }

    @Test
    void validateDeposit_throwsForClosedDestinationAccount() {
        destination.setIsActive(false);

        assertThrows(IllegalStateException.class,
                () -> transactionPolicy.validateDeposit(deposit("100.00"), destination, BigDecimal.ZERO));
    }

    private BigDecimal dailyTotal(String amount) {
        return new BigDecimal(amount);
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

    private CustomerProfileModel customer(Long id) {
        CustomerProfileModel customer = new CustomerProfileModel();
        customer.setId(id);
        return customer;
    }
}
