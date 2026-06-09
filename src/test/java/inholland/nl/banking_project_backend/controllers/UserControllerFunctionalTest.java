package inholland.nl.banking_project_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    //Profile endpoints

    @Test
    void getProfile_forAuthenticatedCustomer_returnsOwnProfile() throws Exception {
        UserModel customer = savedUser("ProfileOwner", RoleEnum.ROLE_CUSTOMER, true);

        mockMvc.perform(get("/api/v1/users/profile")
                        .with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.firstName").value("ProfileOwner"));
    }

    @Test
    void updateProfile_withValidPayload_returnsUpdatedProfileAndNewToken() throws Exception {
        UserModel customer = savedUser("UpdateTarget", RoleEnum.ROLE_CUSTOMER, true);
        Map<String, Object> request = Map.of(
                "email", "updated-" + UUID.randomUUID() + "@example.com",
                "phoneNumber", "+31611112222"
        );

        mockMvc.perform(put("/api/v1/users/profile")
                        .with(user(customer))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(request.get("email")))
                .andExpect(jsonPath("$.phoneNumber").value(request.get("phoneNumber")))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    //Registration endpoints (employee-only)

    @Test
    void getRegistrations_forEmployee_returnsPaginatedPendingUsers() throws Exception {
        UserModel employee = savedUser("Employee", RoleEnum.ROLE_EMPLOYEE, true);
        savedPendingUser("PendingOne");
        savedPendingUser("PendingTwo");

        mockMvc.perform(get("/api/v1/users/registrations")
                        .param("status", "PENDING")
                        .with(user(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void getRegistrations_forCustomer_returnsForbidden() throws Exception {
        UserModel customer = savedUser("NosyCustomer", RoleEnum.ROLE_CUSTOMER, true);

        mockMvc.perform(get("/api/v1/users/registrations")
                        .param("status", "PENDING")
                        .with(user(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateRegistrationStatus_approveWithCustomLimits_provisionsAccountsWithThoseLimits() throws Exception {
        UserModel employee = savedUser("Approver", RoleEnum.ROLE_EMPLOYEE, true);
        UserModel pending = savedPendingUser("ApproveMe");

        Map<String, Object> request = Map.of(
                "status", "APPROVED",
                "absoluteLimit", -250,
                "dailyLimit", 750
        );

        mockMvc.perform(patch("/api/v1/users/registrations/{id}", pending.getId())
                        .with(user(employee))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        UserModel updated = userRepository.findById(pending.getId()).orElseThrow();
        assertTrue(updated.getIsApproved());
        assertNotNull(updated.getIban());

        List<inholland.nl.banking_project_backend.models.AccountModel> accounts =
                accountRepository.findAccounts(updated.getEmail(), null, Pageable.unpaged()).getContent();

        assertEquals(2, accounts.size());
        accounts.forEach(account -> {
            assertEquals(0, new BigDecimal("-250.00").compareTo(account.getAbsoluteLimit()));
            assertEquals(0, new BigDecimal("750.00").compareTo(account.getDailyLimit()));
        });
    }

    @Test
    void updateRegistrationStatus_approveWithoutLimits_fallsBackToDefaultLimits() throws Exception {
        UserModel employee = savedUser("ApproverDefault", RoleEnum.ROLE_EMPLOYEE, true);
        UserModel pending = savedPendingUser("DefaultLimits");

        Map<String, Object> request = Map.of("status", "APPROVED");

        mockMvc.perform(patch("/api/v1/users/registrations/{id}", pending.getId())
                        .with(user(employee))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        List<inholland.nl.banking_project_backend.models.AccountModel> accounts =
                accountRepository.findAccounts(pending.getEmail(), null, Pageable.unpaged()).getContent();

        assertEquals(2, accounts.size());
        accounts.forEach(account -> {
            assertEquals(0, new BigDecimal("-500.00").compareTo(account.getAbsoluteLimit()));
            assertEquals(0, new BigDecimal("1000.00").compareTo(account.getDailyLimit()));
        });
    }

    @Test
    void updateRegistrationStatus_deny_removesRegistrationRecord() throws Exception {
        UserModel employee = savedUser("Denier", RoleEnum.ROLE_EMPLOYEE, true);
        UserModel pending = savedPendingUser("DenyMe");

        Map<String, Object> request = Map.of("status", "DENIED");

        mockMvc.perform(patch("/api/v1/users/registrations/{id}", pending.getId())
                        .with(user(employee))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        assertFalse(userRepository.existsById(pending.getId()));
    }

    @Test
    void updateRegistrationStatus_forCustomer_returnsForbidden() throws Exception {
        UserModel customer = savedUser("NotAnEmployee", RoleEnum.ROLE_CUSTOMER, true);
        UserModel pending = savedPendingUser("OffLimits");

        Map<String, Object> request = Map.of("status", "APPROVED");

        mockMvc.perform(patch("/api/v1/users/registrations/{id}", pending.getId())
                        .with(user(customer)).
                        with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private UserModel savedUser(String label, RoleEnum role, boolean isApproved) {
        UserModel user = new UserModel();
        user.setEmail(label.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("password");
        user.setFirstName(label);
        user.setLastName("User");
        user.setBsn("123456782");
        user.setPhoneNumber("+31611111111");
        user.setRole(role);
        user.setIsApproved(isApproved);
        return userRepository.save(user);
    }

    private UserModel savedPendingUser(String label) {
        UserModel user = new UserModel();
        user.setEmail(label.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword("password");
        user.setFirstName(label);
        user.setLastName("Applicant");
        user.setBsn("123456782");
        user.setPhoneNumber("+31611111111");
        user.setRole(RoleEnum.ROLE_CUSTOMER);
        user.setIsApproved(false);
        return userRepository.save(user);
    }
}
