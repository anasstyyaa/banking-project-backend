package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JWTService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // ==========================================
    // REGISTER TESTS
    // ==========================================

    @Test
    void givenValidRegistrationData_whenRegister_shouldReturnLoginResponse() {
        // Arrange
        UserDTO.RegisterRequest request = validRegisterRequest("test@mail.com", "123456782");
        UserModel userEntity = new UserModel();
        userEntity.setEmail("test@mail.com");
        userEntity.setRole(RoleEnum.ROLE_CUSTOMER);

        when(userService.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(userEntity);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userService.create(userEntity)).thenReturn(userEntity);
        when(jwtService.generateToken("test@mail.com", "ROLE_CUSTOMER")).thenReturn("mockedToken");

        // Act
        UserDTO.LoginResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@mail.com", response.email());
        assertEquals("mockedToken", response.token());
        assertEquals(RoleEnum.ROLE_CUSTOMER, response.role());
        verify(userService).create(userEntity);
    }

    @Test
    void givenExistingEmail_whenRegister_shouldThrowConflictException() {
        // Arrange
        UserDTO.RegisterRequest request = validRegisterRequest("existing@mail.com", "123456782");
        when(userService.existsByEmail(request.email())).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.register(request);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());
        verify(userService, never()).create(any());
    }

    @Test
    void givenInvalidBsn_whenRegister_shouldThrowBadRequestException() {
        // Arrange
        UserDTO.RegisterRequest request = validRegisterRequest("test@mail.com", "123456789");
        when(userService.existsByEmail(request.email())).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.register(request);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("bsn: The provided BSN is mathematically invalid", exception.getReason());
        verify(userService, never()).create(any());
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    void givenValidCredentials_whenLogin_shouldReturnLoginResponse() {
        // Arrange
        UserDTO.LoginRequest request = new UserDTO.LoginRequest("test@mail.com", "password123");
        UserModel userEntity = new UserModel();
        userEntity.setEmail("test@mail.com");
        userEntity.setRole(RoleEnum.ROLE_CUSTOMER);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userService.findByEmail(request.email())).thenReturn(userEntity);
        when(jwtService.generateToken("test@mail.com", "ROLE_CUSTOMER")).thenReturn("mockedToken");

        // Act
        UserDTO.LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("test@mail.com", response.email());
        assertEquals("mockedToken", response.token());
        assertEquals(RoleEnum.ROLE_CUSTOMER, response.role());
    }

    @Test
    void givenPendingAccount_whenLogin_shouldThrowForbiddenException() {
        // Arrange
        UserDTO.LoginRequest request = new UserDTO.LoginRequest("pending@mail.com", "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new DisabledException("Disabled"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("ACCOUNT_PENDING_APPROVAL", exception.getReason());
        verifyNoInteractions(jwtService);
    }

    @Test
    void givenBadCredentials_whenLogin_shouldThrowUnauthorizedException() {
        // Arrange
        UserDTO.LoginRequest request = new UserDTO.LoginRequest("wrong@mail.com", "wrongpass");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(request);
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
        verifyNoInteractions(jwtService);
    }

    private UserDTO.RegisterRequest validRegisterRequest(String email, String bsn) {
        return new UserDTO.RegisterRequest(
                email,
                "Password123!",
                bsn,
                "Test",
                "User",
                "+31612345678"
        );
    }
}
