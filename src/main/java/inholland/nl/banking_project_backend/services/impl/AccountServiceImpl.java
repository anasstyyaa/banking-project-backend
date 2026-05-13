package inholland.nl.banking_project_backend.services.impl;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.enums.RoleEnum;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.mapper.AccountServiceMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.services.AccountService;
import inholland.nl.banking_project_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountServiceMapper accountServiceMapper;
    private final UserService userService;

    // Returns all accounts visible to the authenticated user.
    @Override
    public List<AccountDTO.AccountResponse> getAccountsForUser(String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        return getVisibleAccounts(user).stream()
                .map(accountServiceMapper::toResponse)
                .toList();
    }

    // Returns a single account only when the user is allowed to view it.
    @Override
    public AccountDTO.AccountResponse getAccountForUser(String iban, String userEmail) {
        UserModel user = userService.findByEmail(userEmail);
        AccountModel account = getActiveAccount(iban);
        validateCanViewAccount(user, account);
        return accountServiceMapper.toResponse(account);
    }

    // Loads an active account by IBAN for service-layer business operations.
    @Override
    public AccountModel getActiveAccount(String iban) {
        AccountModel account = findByIban(iban);
        validateAccountIsActive(account);
        return account;
    }

    // Verifies that the user may use this account in a protected operation.
    @Override
    public void validateCanUseAccount(UserModel user, AccountModel account) {
        validateCanViewAccount(user, account);
    }

    // Subtracts money from an account in memory.
    @Override
    public void debit(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().subtract(amount));
    }

    // Adds money to an account in memory.
    @Override
    public void credit(AccountModel account, BigDecimal amount) {
        account.setBalance(account.getBalance().add(amount));
    }

    // Persists a changed account.
    @Override
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
}
