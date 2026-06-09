package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionFilterRequestDTO;
import inholland.nl.banking_project_backend.dtos.TransactionResponseDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.mappers.TransactionMapper;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.policies.TransactionPolicy;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private TransactionPolicy transactionPolicy;

    @InjectMocks
    private TransactionService transactionService;

    private AccountModel source;
    private AccountModel destination;
    private UserModel user;
    private TransactionModel transaction;
    private TransactionResponseDTO response;

    @BeforeEach
    void setUp() {
        source = account("NL01INHO000000001", "1000.00");
        destination = account("NL01INHO000000002", "250.00");

        user = new UserModel();
        user.setEmail("customer@example.com");

        transaction = new TransactionModel();
        transaction.setId(1L);
        transaction.setType(TransactionTypeEnum.TRANSFER);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedBy(user);

        response = new TransactionResponseDTO(
                1L,
                TransactionTypeEnum.TRANSFER,
                source.getIban(),
                destination.getIban(),
                new BigDecimal("100.00"),
                transaction.getTimestamp(),
                user.getEmail()
        );
    }

    @Test
    void createTransaction_customerTransfer_movesMoneyAndSavesTransaction() {
        CreateTransactionRequestDTO request = transferRequest("100.00");
        BigDecimal dailyOutgoingTotal = new BigDecimal("25.00");
        when(accountRepository.findAccountByIban(source.getIban(), user.getEmail())).thenReturn(Optional.of(source));
        when(accountRepository.findAccountByIban(destination.getIban(), null)).thenReturn(Optional.of(destination));
        when(transactionRepository.sumOutgoingAmountForAccount(any(), any(), any(), any())).thenReturn(dailyOutgoingTotal);
        when(transactionMapper.toModel(request, source, destination, user)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.createTransaction(request, user, user.getEmail());

        assertEquals(response, result);
        assertEquals(new BigDecimal("900.00"), source.getBalance());
        assertEquals(new BigDecimal("350.00"), destination.getBalance());
        verify(transactionPolicy).validateCustomerTransfer(request, source, destination, dailyOutgoingTotal);
        verify(accountRepository).save(source);
        verify(accountRepository).save(destination);
        verify(transactionRepository).save(transaction);
    }

    @Test
    void createTransaction_employeeTransfer_usesEmployeePolicy() {
        CreateTransactionRequestDTO request = transferRequest("100.00");
        BigDecimal dailyOutgoingTotal = new BigDecimal("50.00");
        when(accountRepository.findAccountByIban(source.getIban(), null)).thenReturn(Optional.of(source));
        when(accountRepository.findAccountByIban(destination.getIban(), null)).thenReturn(Optional.of(destination));
        when(transactionRepository.sumOutgoingAmountForAccount(any(), any(), any(), any())).thenReturn(dailyOutgoingTotal);
        when(transactionMapper.toModel(request, source, destination, user)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        transactionService.createTransaction(request, user, null);

        verify(transactionPolicy).validateEmployeeTransfer(request, source, destination, dailyOutgoingTotal);
    }

    @Test
    void createTransaction_deposit_updatesDestinationBalance() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                TransactionTypeEnum.DEPOSIT,
                null,
                destination.getIban(),
                new BigDecimal("50.00")
        );
        BigDecimal dailyDepositTotal = new BigDecimal("100.00");
        when(accountRepository.findAccountByIban(destination.getIban(), user.getEmail())).thenReturn(Optional.of(destination));
        when(transactionRepository.sumDepositAmountForAccount(any(), any(), any(), any())).thenReturn(dailyDepositTotal);
        when(transactionMapper.toModel(request, null, destination, user)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        transactionService.createTransaction(request, user, user.getEmail());

        assertEquals(new BigDecimal("300.00"), destination.getBalance());
        verify(transactionPolicy).validateDeposit(request, destination, dailyDepositTotal);
        verify(accountRepository).save(destination);
    }

    @Test
    void createTransaction_withdrawal_updatesSourceBalance() {
        CreateTransactionRequestDTO request = new CreateTransactionRequestDTO(
                TransactionTypeEnum.WITHDRAWAL,
                source.getIban(),
                null,
                new BigDecimal("75.00")
        );
        BigDecimal dailyOutgoingTotal = new BigDecimal("125.00");
        when(accountRepository.findAccountByIban(source.getIban(), user.getEmail())).thenReturn(Optional.of(source));
        when(transactionRepository.sumOutgoingAmountForAccount(any(), any(), any(), any())).thenReturn(dailyOutgoingTotal);
        when(transactionMapper.toModel(request, source, null, user)).thenReturn(transaction);
        when(transactionRepository.save(transaction)).thenReturn(transaction);
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        transactionService.createTransaction(request, user, user.getEmail());

        assertEquals(new BigDecimal("925.00"), source.getBalance());
        verify(transactionPolicy).validateWithdrawal(request, source, dailyOutgoingTotal);
        verify(accountRepository).save(source);
    }

    @Test
    void createTransaction_policyFailureDoesNotSaveAnything() {
        CreateTransactionRequestDTO request = transferRequest("100.00");
        when(accountRepository.findAccountByIban(source.getIban(), user.getEmail())).thenReturn(Optional.of(source));
        when(accountRepository.findAccountByIban(destination.getIban(), null)).thenReturn(Optional.of(destination));
        when(transactionRepository.sumOutgoingAmountForAccount(any(), any(), any(), any())).thenReturn(BigDecimal.ZERO);
        doThrow(new IllegalArgumentException("Policy rejected"))
                .when(transactionPolicy).validateCustomerTransfer(request, source, destination, BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> transactionService.createTransaction(request, user, user.getEmail()));

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createTransaction_throwsWhenSourceAccountIsMissing() {
        CreateTransactionRequestDTO request = transferRequest("100.00");
        when(accountRepository.findAccountByIban(source.getIban(), user.getEmail())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> transactionService.createTransaction(request, user, user.getEmail()));

        verify(transactionPolicy, never()).validateCustomerTransfer(any(), any(), any(), any());
    }

    @Test
    void getTransactions_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        TransactionFilterRequestDTO filter = new TransactionFilterRequestDTO(
                null, null, null, null, null, "", null
        );
        when(transactionRepository.findTransactions(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        Page<TransactionResponseDTO> result = transactionService.getTransactions(filter, user.getEmail(), pageable);

        assertEquals(response, result.getContent().getFirst());
    }

    @Test
    void getTransaction_returnsMappedTransaction() {
        when(transactionRepository.findTransactionById(1L, user.getEmail())).thenReturn(Optional.of(transaction));
        when(transactionMapper.toResponse(transaction)).thenReturn(response);

        TransactionResponseDTO result = transactionService.getTransaction(1L, user.getEmail());

        assertEquals(response, result);
    }

    private CreateTransactionRequestDTO transferRequest(String amount) {
        return new CreateTransactionRequestDTO(
                TransactionTypeEnum.TRANSFER,
                source.getIban(),
                destination.getIban(),
                new BigDecimal(amount)
        );
    }

    private AccountModel account(String iban, String balance) {
        AccountModel account = new AccountModel();
        account.setIban(iban);
        account.setType(AccountTypeEnum.CHECKING);
        account.setBalance(new BigDecimal(balance));
        account.setAbsoluteLimit(new BigDecimal("-500.00"));
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setIsActive(true);
        return account;
    }
}
