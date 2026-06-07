package inholland.nl.banking_project_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerFunctionalTest {

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

    // Employee account listing returns a Spring page response.
    @Test
    void getAccounts_forEmployee_returnsPaginatedAccounts() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        CustomerProfileModel profile = savedProfile(savedUser("customer", RoleEnum.ROLE_CUSTOMER));
        savedAccount(profile, "NL11INHO000000101", AccountTypeEnum.CHECKING);

        mockMvc.perform(get("/api/v1/accounts")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.size").value(20));
    }

    // Customer account listing is scoped to the authenticated customer only.
    @Test
    void getAccounts_forCustomer_returnsOnlyOwnAccounts() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        CustomerProfileModel ownProfile = savedProfile(customer);
        CustomerProfileModel otherProfile = savedProfile(savedUser("other", RoleEnum.ROLE_CUSTOMER));
        savedAccount(ownProfile, "NL11INHO000000102", AccountTypeEnum.CHECKING);
        savedAccount(otherProfile, "NL11INHO000000103", AccountTypeEnum.CHECKING);

        mockMvc.perform(get("/api/v1/accounts")
                        .with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].iban").value("NL11INHO000000102"));
    }

    // Customers cannot retrieve another customer's account by IBAN.
    @Test
    void getAccount_forCustomerWhenAccountBelongsToSomeoneElse_returnsNotFound() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        CustomerProfileModel otherProfile = savedProfile(savedUser("other", RoleEnum.ROLE_CUSTOMER));
        savedAccount(otherProfile, "NL11INHO000000104", AccountTypeEnum.CHECKING);

        mockMvc.perform(get("/api/v1/accounts/{iban}", "NL11INHO000000104")
                        .with(user(customer)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Account not found."));
    }

    // Search supports the user story for finding another customer's IBAN by name and stays lightweight.
    @Test
    void searchAccounts_returnsLightweightIbanLookupResponse() throws Exception {
        UserModel customer = savedUser("searchtarget", RoleEnum.ROLE_CUSTOMER);
        customer.setFirstName("Lookup");
        customer.setLastName("Person");
        userRepository.save(customer);
        CustomerProfileModel profile = savedProfile(customer);
        savedAccount(profile, "NL11INHO000000105", AccountTypeEnum.CHECKING);

        mockMvc.perform(get("/api/v1/accounts/search")
                        .param("query", "Lookup")
                        .with(user(savedUser("viewer", RoleEnum.ROLE_CUSTOMER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].iban").value("NL11INHO000000105"))
                .andExpect(jsonPath("$.content[0].customerName").value("Lookup Person"))
                .andExpect(jsonPath("$.content[0].balance").doesNotExist())
                .andExpect(jsonPath("$.content[0].dailyLimit").doesNotExist())
                .andExpect(jsonPath("$.content[0].customerEmail").doesNotExist());
    }

    // Employees can create a customer account through the account resource endpoint.
    @Test
    void createAccount_forEmployee_returnsCreatedAccount() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        UserModel customer = savedUser("newaccount", RoleEnum.ROLE_CUSTOMER);
        savedProfile(customer);
        Map<String, Object> request = Map.of(
                "customerUserId", customer.getId(),
                "accountType", "CHECKING",
                "absoluteLimit", -500,
                "dailyLimit", 1000
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.iban").exists())
                .andExpect(jsonPath("$.type").value("CHECKING"))
                .andExpect(jsonPath("$.dailyLimit").value(1000));
    }

    // Customers are blocked from employee-only account creation.
    @Test
    void createAccount_forCustomer_returnsForbidden() throws Exception {
        UserModel customer = savedUser("customer", RoleEnum.ROLE_CUSTOMER);
        Map<String, Object> request = Map.of(
                "customerUserId", customer.getId(),
                "accountType", "CHECKING",
                "absoluteLimit", -500,
                "dailyLimit", 1000
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .with(csrf())
                        .with(user(customer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // DTO validation returns bad request for invalid limit updates.
    @Test
    void updateLimits_withInvalidDailyLimit_returnsBadRequest() throws Exception {
        UserModel employee = savedUser("employee", RoleEnum.ROLE_EMPLOYEE);
        CustomerProfileModel profile = savedProfile(savedUser("customer", RoleEnum.ROLE_CUSTOMER));
        savedAccount(profile, "NL11INHO000000106", AccountTypeEnum.CHECKING);
        Map<String, Object> request = Map.of(
                "absoluteLimit", -500,
                "dailyLimit", 0
        );

        mockMvc.perform(patch("/api/v1/accounts/{iban}/limits", "NL11INHO000000106")
                        .with(csrf())
                        .with(user(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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

    private AccountModel savedAccount(CustomerProfileModel profile, String iban, AccountTypeEnum type) {
        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(iban);
        account.setType(type);
        account.setBalance(new BigDecimal("1000.00"));
        account.setAbsoluteLimit(new BigDecimal("-500.00"));
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setIsActive(true);
        return accountRepository.save(account);
    }
}
