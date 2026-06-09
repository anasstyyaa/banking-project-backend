package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountResponseDTO;
import inholland.nl.banking_project_backend.dtos.AccountSearchResponseDTO;
import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.exceptions.CustomerProfileNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

    @Transactional(rollbackOn = Exception.class)
    public AccountResponseDTO createAccount(CreateAccountRequestDTO request) {
        UserModel user = userRepository.findById(request.customerUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.customerUserId()));
        accountPolicy.validateAccountCreation(user, request);

        CustomerProfileModel profile = customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseThrow(() -> new CustomerProfileNotFoundException("Customer profile not found for user: " + user.getEmail()));

        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(ibanGenerator.generateDutchIban());
        account.setType(request.accountType());
        account.setBalance(BigDecimal.ZERO);
        account.setAbsoluteLimit(request.absoluteLimit());
        account.setDailyLimit(request.dailyLimit());
        account.setIsActive(true);

        AccountModel savedAccount = accountRepository.save(account);
        log.info("Created {} account for customer user ID: {}", request.accountType(), request.customerUserId());
        return accountMapper.toResponse(savedAccount);
    }

    public Page<AccountResponseDTO> getAccounts(String ownerEmail, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return accountRepository.searchAccounts(search, pageable)
                    .map(accountMapper::toResponse);
        }
        return accountRepository.findAccounts(ownerEmail,null, pageable)
                .map(accountMapper::toResponse);
    }

    public AccountResponseDTO getAccount(String iban, String ownerEmail) {
        AccountModel account = accountRepository.findAccountByIban(iban, ownerEmail)
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));
        accountPolicy.requireOpenAccount(account);
        return accountMapper.toResponse(account);
    }

    @Transactional
    public AccountResponseDTO updateLimits(String iban, UpdateAccountLimitsRequestDTO request) {
        AccountModel account = accountRepository.findAccountByIban(iban, null)
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));
        accountPolicy.validateLimitUpdate(account, request);
        account.setAbsoluteLimit(request.absoluteLimit());
        account.setDailyLimit(request.dailyLimit());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    @Transactional
    public AccountResponseDTO closeAccount(String iban) {
        AccountModel account = accountRepository.findAccountByIban(iban, null)
                .orElseThrow(() -> new EntityNotFoundException("Account not found."));
        accountPolicy.validateAccountClosure(account);
        account.setIsActive(false);
        return accountMapper.toResponse(accountRepository.save(account));
    }

    public Page<AccountSearchResponseDTO> searchAccounts(String term, Pageable pageable) {
        return accountRepository.searchAccounts(term, pageable)
                .map(accountMapper::toSearchResponse);
    }

}
