package inholland.nl.banking_project_backend.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JWTServiceTest {

    @InjectMocks
    private JWTService jwtService;

    private final String testEmail = "bank.user@inholland.nl";
    private final String testRole = "ROLE_CUSTOMER";

    @BeforeEach
    void setUp() {
        // Manually injecting the secret key value because @Value is ignored in pure unit tests
        // Secret must be at least 256 bits (32 characters/bytes) for HS256 encryption
        String secureMockKey = "abcdefghijklmnopqrstuvwxyz1234567890abcdefghijkl";
        ReflectionTestUtils.setField(jwtService, "secretKey", secureMockKey);
    }

    // ==========================================
    // GENERATION & EXTRACTION TESTS
    // ==========================================

    @Test
    void givenEmailAndRole_whenGenerateToken_shouldCreateValidJwtString() {
        // Act
        String token = jwtService.generateToken(testEmail, testRole);

        // Assert
        assertNotNull(token);
        assertFalse(token.trim().isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void givenValidToken_whenExtractEmail_shouldReturnCorrectSubject() {
        // Arrange
        String token = jwtService.generateToken(testEmail, testRole);

        // Act
        String extractedEmail = jwtService.extractEmail(token);

        // Assert
        assertEquals(testEmail, extractedEmail);
    }

    // ==========================================
    // VALIDATION TESTS
    // ==========================================

    @Test
    void givenMatchingEmailAndActiveToken_whenIsTokenValid_shouldReturnTrue() {
        // Arrange
        String token = jwtService.generateToken(testEmail, testRole);

        // Act
        boolean isValid = jwtService.isTokenValid(token, testEmail);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void givenMismatchedEmail_whenIsTokenValid_shouldReturnFalse() {
        // Arrange
        String token = jwtService.generateToken(testEmail, testRole);
        String wrongEmail = "intruder@mail.com";

        // Act
        boolean isValid = jwtService.isTokenValid(token, wrongEmail);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void givenTamperedToken_whenExtractAllClaims_shouldThrowSignatureException() {
        // Arrange
        String validToken = jwtService.generateToken(testEmail, testRole);
        String tamperedToken = validToken + "manipulatedData";

        // Act & Assert
        assertThrows(SignatureException.class, () -> {
            jwtService.extractEmail(tamperedToken);
        });
    }
}