package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.AccountResponseDTO;
import inholland.nl.banking_project_backend.dtos.AccountSearchResponseDTO;
import inholland.nl.banking_project_backend.dtos.CreateAccountRequestDTO;
import inholland.nl.banking_project_backend.dtos.UpdateAccountLimitsRequestDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private IbanGenerator ibanGenerator;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private AccountPolicy accountPolicy;

    @InjectMocks
    private AccountService accountService;

    private UserModel customer;
    private CustomerProfileModel profile;
    private AccountModel account;
    private AccountResponseDTO accountResponse;

    @BeforeEach
    void setUp() {
        customer = new UserModel();
        customer.setId(1L);
        customer.setEmail("customer@example.com");
        customer.setIsApproved(true);

        profile = new CustomerProfileModel();
        profile.setUser(customer);

        account = account("NL01INHO000000001");
        account.setCustomer(profile);

        accountResponse = new AccountResponseDTO(
                account.getIban(),
                AccountTypeEnum.CHECKING,
                account.getBalance(),
                true,
                account.getAbsoluteLimit(),
                account.getDailyLimit(),
                customer.getEmail(),
                "Customer Example"
        );
    }

    @Test
    void createAccount_savesAccountForApprovedCustomer() {
        CreateAccountRequestDTO request = createAccountRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerProfileRepository.findByUserEmail(customer.getEmail())).thenReturn(Optional.of(profile));
        when(ibanGenerator.generateDutchIban()).thenReturn(account.getIban());
        when(accountRepository.save(any(AccountModel.class))).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponseDTO result = accountService.createAccount(request);

        assertEquals(accountResponse, result);
        verify(accountPolicy).validateAccountCreation(customer, request);
        verify(accountRepository).save(any(AccountModel.class));
    }

    @Test
    void createAccount_throwsWhenUserIsMissing() {
        CreateAccountRequestDTO request = createAccountRequest();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> accountService.createAccount(request));

        verify(accountPolicy, never()).validateAccountCreation(any(), any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void getAccounts_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(accountRepository.findAccounts("customer@example.com", null, pageable))
                .thenReturn(new PageImpl<>(List.of(account), pageable, 1));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        Page<AccountResponseDTO> result = accountService.getAccounts("customer@example.com", null, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(accountResponse, result.getContent().getFirst());
    }

    @Test
    void getAccount_returnsMappedAccount() {
        when(accountRepository.findAccountByIban(account.getIban(), customer.getEmail())).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponseDTO result = accountService.getAccount(account.getIban(), customer.getEmail());

        assertEquals(accountResponse, result);
        verify(accountPolicy).requireOpenAccount(account);
    }

    @Test
    void updateLimits_savesUpdatedLimits() {
        UpdateAccountLimitsRequestDTO request = new UpdateAccountLimitsRequestDTO(
                new BigDecimal("-250.00"),
                new BigDecimal("750.00")
        );
        when(accountRepository.findAccountByIban(account.getIban(), null)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponseDTO result = accountService.updateLimits(account.getIban(), request);

        assertEquals(accountResponse, result);
        assertEquals(new BigDecimal("-250.00"), account.getAbsoluteLimit());
        assertEquals(new BigDecimal("750.00"), account.getDailyLimit());
        verify(accountPolicy).validateLimitUpdate(account, request);
    }

    @Test
    void searchAccounts_returnsMappedSearchPage() {
        Pageable pageable = PageRequest.of(0, 10);
        AccountSearchResponseDTO searchResponse = new AccountSearchResponseDTO(account.getIban(), AccountTypeEnum.CHECKING, "Customer Example");
        when(accountRepository.searchAccounts("customer", pageable)).thenReturn(new PageImpl<>(List.of(account), pageable, 1));
        when(accountMapper.toSearchResponse(account)).thenReturn(searchResponse);

        Page<AccountSearchResponseDTO> result = accountService.searchAccounts("customer", pageable);

        assertEquals(searchResponse, result.getContent().getFirst());
    }

    @Test
    void closeAccount_marksAccountInactiveAndSaves() {
        account.setBalance(BigDecimal.ZERO);
        when(accountRepository.findAccountByIban(account.getIban(), null)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toResponse(account)).thenReturn(accountResponse);

        AccountResponseDTO result = accountService.closeAccount(account.getIban());

        assertEquals(accountResponse, result);
        assertEquals(false, account.getIsActive());
        verify(accountPolicy).validateAccountClosure(account);
        verify(accountRepository).save(account);
    }

    @Test
    void closeAccount_policyFailureDoesNotSave() {
        when(accountRepository.findAccountByIban(account.getIban(), null)).thenReturn(Optional.of(account));
        doThrow(new IllegalStateException("Cannot close an account with a non-zero balance."))
                .when(accountPolicy).validateAccountClosure(account);

        assertThrows(IllegalStateException.class, () -> accountService.closeAccount(account.getIban()));

        verify(accountRepository, never()).save(any());
    }

    private CreateAccountRequestDTO createAccountRequest() {
        return new CreateAccountRequestDTO(
                1L,
                AccountTypeEnum.CHECKING,
                new BigDecimal("-500.00"),
                new BigDecimal("1000.00")
        );
    }

    private AccountModel account(String iban) {
        AccountModel account = new AccountModel();
        account.setIban(iban);
        account.setType(AccountTypeEnum.CHECKING);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteLimit(new BigDecimal("-500.00"));
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setIsActive(true);
        return account;
    }
}
