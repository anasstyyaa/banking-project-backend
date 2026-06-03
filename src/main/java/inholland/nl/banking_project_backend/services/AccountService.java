package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.mappers.AccountMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.exceptions.InactiveAccountException;
import inholland.nl.banking_project_backend.exceptions.UnauthorizedAccountAccessException;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import inholland.nl.banking_project_backend.utils.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;
    private final UserService userService;
    private final IbanGenerator idanGenerator;
    private final CustomerProfileRepository customerProfileRepository;

    @Transactional(rollbackOn = Exception.class)
    public AccountDTO.AccountResponse createAdditionalAccount(Long userId, AccountTypeEnum type) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!user.getIsApproved()) {
            throw new IllegalStateException("Cannot open portfolios for unapproved banking registration records.");
        }

        CustomerProfileModel profile = customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Customer financial profile context data missing."));

        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(idanGenerator.generateDutchIban());
        account.setType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setAbsoluteLimit(new BigDecimal("-500.00"));
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setIsActive(true);

        AccountModel savedAccount = accountRepository.save(account);
        log.info("Provisioned a secondary {} account for customer user identity ID: {}", type, userId);
        return accountMapper.toResponse(savedAccount);
    }

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
