package inholland.nl.banking_project_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.TransactionModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.TransactionRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TransactionControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Transaction history returns a paginated API response.
    @Test
    void getTransactions_returnsPaginatedResponse() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        AccountModel source = savedAccount(savedProfile(savedUser("source", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000101", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000102", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        savedTransaction(source, destination, employee, "100.00");

        mockMvc.perform(get("/api/v1/transactions")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.size").value(20));
    }

    // Customers only see transactions connected to their own accounts.
    @Test
    void getTransactions_forCustomer_returnsOnlyVisibleTransactions() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        CustomerProfileModel profile = savedProfile(customer);
        AccountModel ownAccount = savedAccount(profile, "NL12INHO0000000103", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel otherAccount = savedAccount(savedProfile(savedUser("other", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000104", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel unrelatedAccount = savedAccount(savedProfile(savedUser("unrelated", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000105", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        savedTransaction(ownAccount, otherAccount, customer, "100.00");
        savedTransaction(otherAccount, unrelatedAccount, customer, "75.00");

        mockMvc.perform(get("/api/v1/transactions")
                        .with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromIban").value("NL12INHO0000000103"));
    }

    // Employee transaction history applies date, amount, IBAN, and pagination filters in the API.
    @Test
    void getTransactions_withDateAmountAndIbanFilters_returnsMatchingTransaction() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        AccountModel source = savedAccount(savedProfile(savedUser("source", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000116", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000117", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        savedTransaction(source, destination, employee, "123.45", LocalDateTime.of(2026, 1, 15, 10, 0));
        savedTransaction(source, destination, employee, "200.00", LocalDateTime.of(2026, 1, 15, 11, 0));

        mockMvc.perform(get("/api/v1/transactions")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .param("amountEqualTo", "123.45")
                        .param("iban", source.getIban())
                        .param("page", "0")
                        .param("size", "20")
                        .with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromIban").value(source.getIban()))
                .andExpect(jsonPath("$.content[0].amount").value(123.45));
    }

    // Employee transaction history can be scoped to one customer by user id.
    @Test
    void getTransactions_withCustomerUserIdFilter_returnsOnlyThatCustomerTransactions() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel customerAccount = savedAccount(savedProfile(customer), "NL12INHO0000000118", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel otherAccount = savedAccount(savedProfile(savedUser("other", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000119", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000120", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        savedTransaction(customerAccount, destination, employee, "50.00");
        savedTransaction(otherAccount, destination, employee, "60.00");

        mockMvc.perform(get("/api/v1/transactions")
                        .param("userId", customer.getId().toString())
                        .with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fromIban").value(customerAccount.getIban()));
    }

    // Customer transfers persist a transaction and return created.
    @Test
    void createTransaction_customerTransfer_returnsCreated() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel source = savedAccount(savedProfile(customer), "NL12INHO0000000106", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000107", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.fromIban").value(source.getIban()))
                .andExpect(jsonPath("$.toIban").value(destination.getIban()))
                .andExpect(jsonPath("$.amount").value(100));
    }

    // Customers can move money between their own savings and checking accounts.
    @Test
    void createTransaction_customerOwnSavingsToCheckingTransfer_returnsCreated() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        CustomerProfileModel profile = savedProfile(customer);
        AccountModel source = savedAccount(profile, "NL12INHO0000000121", AccountTypeEnum.SAVINGS, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(profile, "NL12INHO0000000122", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromIban").value(source.getIban()))
                .andExpect(jsonPath("$.toIban").value(destination.getIban()));
    }

    // External customer transfers cannot use savings accounts.
    @Test
    void createTransaction_customerExternalSavingsTransfer_returnsBadRequest() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel source = savedAccount(savedProfile(customer), "NL12INHO0000000123", AccountTypeEnum.SAVINGS, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000124", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("External customer transfers can only be made between checking accounts."));
    }

    // Employee transfers are allowed only between checking accounts.
    @Test
    void createTransaction_employeeCheckingTransfer_returnsCreated() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        AccountModel source = savedAccount(savedProfile(savedUser("source", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000108", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000109", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.initiatedByEmail").value(employee.getEmail()));
    }

    // Staff cannot use the transaction endpoint for ATM deposits or withdrawals.
    @Test
    void createTransaction_employeeDeposit_returnsBadRequest() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000110", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "DEPOSIT",
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only transfers can be created for customer accounts by staff."));
    }

    // ATM deposits through the transaction API can only target checking accounts.
    @Test
    void createTransaction_customerDepositToSavings_returnsBadRequest() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel destination = savedAccount(savedProfile(customer), "NL12INHO0000000125", AccountTypeEnum.SAVINGS, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "DEPOSIT",
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ATM deposits can only be made into checking accounts."));
    }

    // ATM withdrawals through the transaction API can only use checking accounts.
    @Test
    void createTransaction_customerWithdrawalFromSavings_returnsBadRequest() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel source = savedAccount(savedProfile(customer), "NL12INHO0000000126", AccountTypeEnum.SAVINGS, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "WITHDRAWAL",
                "fromIban", source.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ATM withdrawals can only be made from checking accounts."));
    }

    // Daily outgoing limit violations are returned as unprocessable entity.
    @Test
    void createTransaction_whenDailyLimitExceeded_returnsUnprocessableEntity() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel source = savedAccount(savedProfile(customer), "NL12INHO0000000111", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "100.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000112", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 150
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This transaction exceeds the account daily limit."));
    }

    // ATM deposit daily limit violations are returned as unprocessable entity.
    @Test
    void createTransaction_whenDepositDailyLimitExceeded_returnsUnprocessableEntity() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel destination = savedAccount(savedProfile(customer), "NL12INHO0000000127", AccountTypeEnum.CHECKING, "1000.00", "-500.00", "100.00");
        savedDepositTransaction(destination, customer, "80.00");
        Map<String, Object> request = Map.of(
                "type", "DEPOSIT",
                "toIban", destination.getIban(),
                "amount", 30
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This transaction exceeds the account daily limit."));
    }

    // Absolute limit violations are returned as unprocessable entity.
    @Test
    void createTransaction_whenAbsoluteLimitExceeded_returnsUnprocessableEntity() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel source = savedAccount(savedProfile(customer), "NL12INHO0000000113", AccountTypeEnum.CHECKING, "100.00", "0.00", "1000.00");
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000114", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "fromIban", source.getIban(),
                "toIban", destination.getIban(),
                "amount", 150
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("This transaction exceeds the account absolute limit."));
    }

    // Missing transaction account fields are reported as bad request.
    @Test
    void createTransaction_missingTransferSource_returnsBadRequest() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        AccountModel destination = savedAccount(savedProfile(savedUser("destination", RoleEnum.ROLE_CUSTOMER)), "NL12INHO0000000115", AccountTypeEnum.CHECKING, "250.00", "-500.00", "1000.00");
        Map<String, Object> request = Map.of(
                "type", "TRANSFER",
                "toIban", destination.getIban(),
                "amount", 100
        );

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Transfer requires a source IBAN."));
    }

    private UserModel savedUser(String label, RoleEnum role) {
        UserModel user = new UserModel();
        user.setEmail(label + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("password");
        user.setFirstName(label);
        user.setLastName("User");
        user.setRole(role);
        user.setIsApproved(true);
        return userRepository.save(user);
    }

    private CustomerProfileModel savedProfile(UserModel user) {
        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);
        return customerProfileRepository.save(profile);
    }

    private AccountModel savedAccount(CustomerProfileModel profile, String iban, AccountTypeEnum type, String balance, String absoluteLimit, String dailyLimit) {
        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal(balance));
        account.setAbsoluteLimit(new BigDecimal(absoluteLimit));
        account.setDailyLimit(new BigDecimal(dailyLimit));
        account.setIsActive(true);
        return accountRepository.save(account);
    }

    private TransactionModel savedTransaction(AccountModel source, AccountModel destination, UserModel initiatedBy, String amount) {
        return savedTransaction(source, destination, initiatedBy, amount, LocalDateTime.now());
    }

    private TransactionModel savedTransaction(AccountModel source, AccountModel destination, UserModel initiatedBy, String amount, LocalDateTime timestamp) {
        TransactionModel transaction = new TransactionModel();
        transaction.setType(TransactionTypeEnum.TRANSFER);
        transaction.setFromAccount(source);
        transaction.setToAccount(destination);
        transaction.setFromIbanSnapshot(source.getIban());
        transaction.setToIbanSnapshot(destination.getIban());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTimestamp(timestamp);
        transaction.setInitiatedBy(initiatedBy);
        return transactionRepository.save(transaction);
    }

    private TransactionModel savedDepositTransaction(AccountModel destination, UserModel initiatedBy, String amount) {
        TransactionModel transaction = new TransactionModel();
        transaction.setType(TransactionTypeEnum.DEPOSIT);
        transaction.setToAccount(destination);
        transaction.setToIbanSnapshot(destination.getIban());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setInitiatedBy(initiatedBy);
        return transactionRepository.save(transaction);
    }
}
