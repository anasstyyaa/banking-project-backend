package inholland.nl.banking_project_backend.services;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JWTServiceTest {

    @InjectMocks
    private JWTService jwtService;

    private final String testEmail = "bank.user@inholland.nl";
    private final String testRole = "ROLE_CUSTOMER";

    @BeforeEach
    void setUp() {
        
        String secureMockKey = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijkl";
        ReflectionTestUtils.setField(jwtService, "secretKey", secureMockKey);
    }

    // GENERATION & EXTRACTION TESTS

    @Test
    void givenEmailAndRole_whenGenerateToken_shouldCreateValidJwtString() {
        String token = jwtService.generateToken(testEmail, testRole);

        assertNotNull(token);
        assertFalse(token.trim().isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void givenValidToken_whenExtractEmail_shouldReturnCorrectSubject() {
        String token = jwtService.generateToken(testEmail, testRole);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(testEmail, extractedEmail);
    }

    // VALIDATION TESTS

    @Test
    void givenMatchingEmailAndActiveToken_whenIsTokenValid_shouldReturnTrue() {
        String token = jwtService.generateToken(testEmail, testRole);

        boolean isValid = jwtService.isTokenValid(token, testEmail);

        assertTrue(isValid);
    }

    @Test
    void givenMismatchedEmail_whenIsTokenValid_shouldReturnFalse() {
        String token = jwtService.generateToken(testEmail, testRole);
        String wrongEmail = "intruder@mail.com";

        boolean isValid = jwtService.isTokenValid(token, wrongEmail);

        assertFalse(isValid);
    }

    @Test
    void givenTamperedToken_whenExtractAllClaims_shouldThrowSignatureException() {
        String validToken = jwtService.generateToken(testEmail, testRole);
        String tamperedToken = validToken + "manipulatedData";

        assertThrows(SignatureException.class, () -> {
            jwtService.extractEmail(tamperedToken);
        });
    }
}
