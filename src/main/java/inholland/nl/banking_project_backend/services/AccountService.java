package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountDTO;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.mappers.AccountMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.UserModel;
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
    private final IbanGenerator ibanGenerator;
    private final CustomerProfileRepository customerProfileRepository;

    // Creates an additional active account for an approved customer with employee-defined limits.
    @Transactional(rollbackOn = Exception.class)
    public AccountDTO.AccountResponse createAdditionalAccount(Long userId, AccountDTO.AccountCreationRequest request) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (!Boolean.TRUE.equals(user.getIsApproved())) {
            throw new IllegalStateException("Cannot open accounts for an unapproved customer.");
        }

        CustomerProfileModel profile = customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Customer profile not found."));

        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(ibanGenerator.generateDutchIban());
        account.setType(request.accountType());
        account.setBalance(BigDecimal.ZERO);
        account.setAbsoluteLimit(request.absoluteLimit());
        account.setDailyLimit(request.dailyLimit());
        account.setIsActive(true);

        AccountModel savedAccount = accountRepository.save(account);
        log.info("Created {} account for customer user ID: {}", request.accountType(), userId);
        return accountMapper.toResponse(savedAccount);
    }

    // Returns all active accounts for employee account management.
    public List<AccountDTO.AccountResponse> getAllAccounts() {
        return accountRepository.findByIsActiveTrue()
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // Returns active accounts owned by one customer.
    public List<AccountDTO.AccountResponse> getAccountsByCustomerEmail(String email) {
        return accountRepository.findByCustomerUserEmailAndIsActiveTrue(email)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // Returns one active account by IBAN for employee account management.
    public AccountDTO.AccountResponse getAccountByIban(String iban) {
        AccountModel account = findActiveAccount(iban);
        return accountMapper.toResponse(account);
    }

    // Returns one active customer-owned account by IBAN.
    public AccountDTO.AccountResponse getCustomerAccountByIban(String iban, String email) {
        AccountModel account = accountRepository.findByIbanAndCustomerUserEmailAndIsActiveTrue(iban, email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
        return accountMapper.toResponse(account);
    }

    // Updates employee-managed transaction limits for one active account.
    @Transactional
    public AccountDTO.AccountResponse updateLimits(String iban, AccountDTO.UpdateLimitsRequest request) {
        AccountModel account = findActiveAccount(iban);
        account.setAbsoluteLimit(request.absoluteLimit());
        account.setDailyLimit(request.dailyLimit());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    // Searches active accounts with a lightweight response for IBAN lookup.
    public List<AccountDTO.AccountSearchResponse> searchAccounts(String term) {
        return accountRepository.searchActiveAccounts(term)
                .stream()
                .map(accountMapper::toSearchResponse)
                .toList();
    }

    // Loads one active account or raises a clear account error.
    private AccountModel findActiveAccount(String iban) {
        return accountRepository.findByIbanAndIsActiveTrue(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
    }
}
