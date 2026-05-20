package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.mappers.AccountMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final UserService userService;

    // Returns all accounts visible to the authenticated user.
    public List<AccountDTO.AccountResponse> getAccountsForUser(String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        return getVisibleAccounts(user).stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // Returns one account only when the user is allowed to view it.
    public AccountDTO.AccountResponse getAccountForUser(String iban, String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        AccountModel account = getActiveAccount(iban);
        validateCanViewAccount(user, account);
        return accountMapper.toResponse(account);
    }

    // Loads an active account by IBAN for service-layer business operations.
    public AccountModel getActiveAccount(String iban) {
        AccountModel account = findByIban(iban);
        validateAccountIsActive(account);
        return account;
    }

    // Verifies that the user may use this account in a protected operation.
    public void validateCanUseAccount(UserModel user, AccountModel account) {
        validateCanViewAccount(user, account);
    }

    // Subtracts money from an account in memory.
    public void debit(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
    }

    // Adds money to an account in memory.
    public void credit(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

    // Persists a changed account.
    public AccountModel save(AccountModel account) {
        return accountRepository.save(account);
    }

    // Returns all accounts for employees and own accounts for customers.
    private List<AccountModel> getVisibleAccounts(UserModel user) {
        if (user.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return accountRepository.findAll();
        }
        return accountRepository.findByCustomerUserEmail(user.getEmail());
    }

    // Loads an account by IBAN or throws a clean business error.
    private AccountModel findByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
    }

    // Rejects operations against closed or inactive accounts.
    private void validateAccountIsActive(AccountModel account) {
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new InactiveAccountException("This account is inactive.");
        }
    }

    // Allows employees to view all accounts and customers to view only their own.
    private void validateCanViewAccount(UserModel user, AccountModel account) {
        if (user.getRole() == RoleEnum.ROLE_EMPLOYEE) {
            return;
        }
        validateCustomerOwnsAccount(user, account);
    }

    // Rejects customer access to accounts owned by another customer.
    private void validateCustomerOwnsAccount(UserModel user, AccountModel account) {
        String ownerEmail = account.getCustomer().getUser().getEmail();
        if (!ownerEmail.equals(user.getEmail())) {
            throw new UnauthorizedAccountAccessException("You are not allowed to use this account.");
        }
    }

    // search iban by name
    public List<AccountDTO.AccountResponse> searchAccountsByName(String name) {
        return accountRepository
                .findByCustomerUserFirstNameContainingIgnoreCaseOrCustomerUserLastNameContainingIgnoreCase(name, name)
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsActive()))
                .map(accountMapper::toResponse)
                .toList();
    }
}
