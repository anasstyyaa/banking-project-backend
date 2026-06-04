package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountResponseDTO;
import inholland.nl.banking_project_backend.dtos.AccountSearchResponseDTO;
import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.exceptions.AccountNotFoundException;
import inholland.nl.banking_project_backend.mappers.AccountMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.policies.AccountPolicy;
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
    private final AccountPolicy accountPolicy;

    // Creates an additional active account for an approved customer with employee-defined limits.
    @Transactional(rollbackOn = Exception.class)
    public AccountResponseDTO createAdditionalAccount(Long userId, CreateAccountRequestDTO request) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        accountPolicy.requireApprovedCustomer(user);

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
    public List<AccountResponseDTO> getAllAccounts() {
        return accountRepository.findByIsActiveTrue()
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // Returns active accounts owned by one customer.
    public List<AccountResponseDTO> getAccountsByCustomerEmail(String email) {
        return accountRepository.findByCustomerUserEmailAndIsActiveTrue(email)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
    }

    // Returns one active account by IBAN for employee account management.
    public AccountResponseDTO getAccountByIban(String iban) {
        AccountModel account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
        accountPolicy.requireOpenAccount(account);
        return accountMapper.toResponse(account);
    }

    // Returns one active customer-owned account by IBAN.
    public AccountResponseDTO getCustomerAccountByIban(String iban, String email) {
        AccountModel account = accountRepository.findByIbanAndCustomerUserEmail(iban, email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
        accountPolicy.requireOpenAccount(account);
        return accountMapper.toResponse(account);
    }

    // Updates employee-managed transaction limits for one active account.
    @Transactional
    public AccountResponseDTO updateLimits(String iban, UpdateAccountLimitsRequestDTO request) {
        AccountModel account = accountRepository.findByIban(iban)
                .orElseThrow(() -> new AccountNotFoundException("Account not found."));
        accountPolicy.requireOpenAccount(account);
        account.setAbsoluteLimit(request.absoluteLimit());
        account.setDailyLimit(request.dailyLimit());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    // Searches active accounts with a lightweight response for IBAN lookup.
    public List<AccountSearchResponseDTO> searchAccounts(String term) {
        return accountRepository.searchActiveAccounts(term)
                .stream()
                .map(accountMapper::toSearchResponse)
                .toList();
    }
}
