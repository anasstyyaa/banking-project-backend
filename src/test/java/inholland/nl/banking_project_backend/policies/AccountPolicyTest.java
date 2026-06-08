package inholland.nl.banking_project_backend.policies;

import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountPolicyTest {

    private AccountPolicy accountPolicy;
    private UserModel approvedUser;
    private CreateAccountRequestDTO createRequest;

    @BeforeEach
    void setUp() {
        accountPolicy = new AccountPolicy();

        approvedUser = new UserModel();
        approvedUser.setIsApproved(true);

        createRequest = new CreateAccountRequestDTO(
                1L,
                AccountTypeEnum.CHECKING,
                new BigDecimal("-500.00"),
                new BigDecimal("1000.00")
        );
    }

    // Approved customers can receive newly created accounts.
    @Test
    void validateAccountCreation_allowsApprovedCustomer() {
        assertDoesNotThrow(() -> accountPolicy.validateAccountCreation(approvedUser, createRequest));
    }

    // Unapproved customers must not receive bank accounts.
    @Test
    void validateAccountCreation_throwsForUnapprovedCustomer() {
        approvedUser.setIsApproved(false);

        assertThrows(IllegalStateException.class,
                () -> accountPolicy.validateAccountCreation(approvedUser, createRequest));
    }

    // Account creation requires complete limit values.
    @Test
    void validateAccountCreation_throwsWhenLimitsAreMissing() {
        CreateAccountRequestDTO request = new CreateAccountRequestDTO(
                1L,
                AccountTypeEnum.CHECKING,
                null,
                new BigDecimal("1000.00")
        );

        assertThrows(IllegalArgumentException.class,
                () -> accountPolicy.validateAccountCreation(approvedUser, request));
    }

    // Open accounts can receive updated account limits.
    @Test
    void validateLimitUpdate_allowsOpenAccount() {
        AccountModel account = openAccount();
        UpdateAccountLimitsRequestDTO request = new UpdateAccountLimitsRequestDTO(
                new BigDecimal("-250.00"),
                new BigDecimal("500.00")
        );

        assertDoesNotThrow(() -> accountPolicy.validateLimitUpdate(account, request));
    }

    // Closed accounts cannot be used for limit updates.
    @Test
    void validateLimitUpdate_throwsForClosedAccount() {
        AccountModel account = openAccount();
        account.setIsActive(false);
        UpdateAccountLimitsRequestDTO request = new UpdateAccountLimitsRequestDTO(
                new BigDecimal("-250.00"),
                new BigDecimal("500.00")
        );

        assertThrows(IllegalStateException.class,
                () -> accountPolicy.validateLimitUpdate(account, request));
    }

    // Zero-balance open accounts can be closed.
    @Test
    void validateAccountClosure_allowsOpenZeroBalanceAccount() {
        AccountModel account = openAccount();
        account.setBalance(BigDecimal.ZERO);

        assertDoesNotThrow(() -> accountPolicy.validateAccountClosure(account));
    }

    // Accounts with money still on them cannot be closed.
    @Test
    void validateAccountClosure_throwsForNonZeroBalance() {
        AccountModel account = openAccount();
        account.setBalance(new BigDecimal("10.00"));

        assertThrows(IllegalStateException.class,
                () -> accountPolicy.validateAccountClosure(account));
    }

    private AccountModel openAccount() {
        AccountModel account = new AccountModel();
        account.setIsActive(true);
        return account;
    }
}
