package inholland.nl.banking_project_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Test
    void register_withValidPayload_returnsCreatedAndLoginResponse() throws Exception {
        String email = "new-customer-" + UUID.randomUUID() + "@example.com";
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "Password123!",
                "bsn", "123456782",
                "firstName", "New",
                "lastName", "Customer",
                "phoneNumber", "+31612345678"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.isApproved").value(false));

        assertUserPersistedAsPending(email);
    }

    @Test
    void register_withDuplicateEmail_returnsConflict() throws Exception {
        UserModel existing = savedApprovedUser("Existing");

        Map<String, Object> request = Map.of(
                "email", existing.getEmail(),
                "password", "Password123!",
                "bsn", "123456782",
                "firstName", "Duplicate",
                "lastName", "Person",
                "phoneNumber", "+31612345678"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_withMathematicallyInvalidBsn_returnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "bad-bsn-" + UUID.randomUUID() + "@example.com",
                "password", "Password123!",
                "bsn", "123456789",
                "firstName", "Bad",
                "lastName", "Bsn",
                "phoneNumber", "+31612345678"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_withMissingRequiredFields_returnsBadRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "",
                "password", "",
                "bsn", "",
                "firstName", "",
                "lastName", "",
                "phoneNumber", ""
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }


    @Test
    void login_withValidCredentials_returnsOkAndToken() throws Exception {
        String rawPassword = "Password123!";
        UserModel user = savedApprovedUserWithPassword("Approved", rawPassword);

        Map<String, Object> request = Map.of(
                "email", user.getEmail(),
                "password", rawPassword
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.isApproved").value(true));
    }

    @Test
    void login_withBadCredentials_returnsUnauthorized() throws Exception {
        UserModel user = savedApprovedUserWithPassword("WrongPassword", "Password123!");

        Map<String, Object> request = Map.of(
                "email", user.getEmail(),
                "password", "TotallyWrongPassword1!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withUnknownEmail_returnsUnauthorized() throws Exception {
        Map<String, Object> request = Map.of(
                "email", "ghost-" + UUID.randomUUID() + "@example.com",
                "password", "Password123!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withPendingAccount_returnsOkWithApprovalFlagFalse() throws Exception {
        String rawPassword = "Password123!";
        UserModel pending = savedPendingUserWithPassword("PendingLogin", rawPassword);

        Map<String, Object> request = Map.of(
                "email", pending.getEmail(),
                "password", rawPassword
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(pending.getEmail()))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.isApproved").value(false));
    }



    private void assertUserPersistedAsPending(String email) {
        UserModel saved = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(saved.getIsApproved());
        org.junit.jupiter.api.Assertions.assertEquals(RoleEnum.ROLE_CUSTOMER, saved.getRole());
    }

    private UserModel savedApprovedUser(String label) {
        return savedApprovedUserWithPassword(label, "password");
    }

    private UserModel savedApprovedUserWithPassword(String label, String rawPassword) {
        return savedUser(label, RoleEnum.ROLE_CUSTOMER, true, rawPassword);
    }

    private UserModel savedPendingUserWithPassword(String label, String rawPassword) {
        return savedUser(label, RoleEnum.ROLE_CUSTOMER, false, rawPassword);
    }

    private UserModel savedUser(String label, RoleEnum role, boolean isApproved, String rawPassword) {
        UserModel user = new UserModel();
        user.setEmail(label.toLowerCase() + "-" + UUID.randomUUID() + "@example.com");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(label);
        user.setLastName("User");
        user.setBsn("123456782");
        user.setPhoneNumber("+31611111111");
        user.setRole(role);
        user.setIsApproved(isApproved);
        return userRepository.save(user);
    }
}
