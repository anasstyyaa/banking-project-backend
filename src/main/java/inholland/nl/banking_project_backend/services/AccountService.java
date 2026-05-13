package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {
    // Returns accounts visible to the authenticated user.
    List<AccountDTO.AccountResponse> getAccountsForUser(String userEmail);

    // Returns one account when the authenticated user is allowed to view it.
    AccountDTO.AccountResponse getAccountForUser(String iban, String userEmail);

    // Loads an active account by IBAN for service-layer business operations.
    AccountModel getActiveAccount(String iban);

    // Verifies that the authenticated user may use the account as a source or ATM target.
    void validateCanUseAccount(UserModel user, AccountModel account);

    // Subtracts money from an account in memory.
    void debit(AccountModel account, BigDecimal amount);

    // Adds money to an account in memory.
    void credit(AccountModel account, BigDecimal amount);

    // Persists a changed account.
    AccountModel save(AccountModel account);
}
