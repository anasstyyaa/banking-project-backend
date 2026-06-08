package inholland.nl.banking_project_backend.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AuthenticationSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MvcResult result;

    @Given("an approved {string} user exists with email {string} and password {string}")
    public void approvedUserExists(String role, String email, String password) {
        saveUser(role, email, password, true);
    }

    @Given("a pending {string} user exists with email {string} and password {string}")
    public void pendingUserExists(String role, String email, String password) {
        saveUser(role, email, password, false);
    }

    @When("the user logs in with email {string} and password {string}")
    public void userLogsIn(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginBody(email, password));

        result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    @Then("the response status should be {int}")
    public void responseStatusShouldBe(int expectedStatus) {
        assertEquals(expectedStatus, result.getResponse().getStatus());
    }

    @Then("the response should contain a JWT token")
    public void responseShouldContainJwtToken() throws Exception {
        JsonNode body = responseBody();

        assertNotNull(body.get("token"));
        assertFalse(body.get("token").asText().isBlank());
    }

    @Then("the response role should be {string}")
    public void responseRoleShouldBe(String expectedRole) throws Exception {
        JsonNode body = responseBody();

        assertEquals(expectedRole, body.get("role").asText());
    }

    @Then("the response approval flag should be false")
    public void responseApprovalFlagShouldBeFalse() throws Exception {
        JsonNode body = responseBody();

        assertFalse(body.get("isApproved").asBoolean());
    }

    private void saveUser(String role, String email, String password, boolean approved) {
        userRepository.findByEmail(email).ifPresent(userRepository::delete);

        UserModel user = new UserModel();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFirstName("BDD");
        user.setLastName("User");
        user.setBsn("123456782");
        user.setPhoneNumber("+31612345678");
        user.setRole(RoleEnum.valueOf(role));
        user.setIsApproved(approved);

        userRepository.save(user);
    }

    private JsonNode responseBody() throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private record LoginBody(String email, String password) {
    }
}
