package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import inholland.nl.banking_project_backend.utils.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private IbanGenerator ibanGenerator;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private UserService userService;

    // SECURITY UTILITY METHOD
    private void mockSecurityContext(String email) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn(email);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    
    // USERDETAILS & CORE CRUD TESTS

    @Test
    void givenExistingEmail_whenLoadUserByUsername_shouldReturnUserDetails() {
        String email = "test@mail.com";
        UserModel user = new UserModel();
        user.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername(email);

        
        assertNotNull(result);
        assertEquals(email, result.getUsername());
    }

    @Test
    void givenNonExistingEmail_whenLoadUserByUsername_shouldThrowUsernameNotFoundException() {
        String email = "missing@mail.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(email));
    }

    @Test
    void givenUserEntity_whenCreate_shouldSaveAndReturnUser() {
        UserModel user = new UserModel();
        when(userRepository.save(user)).thenReturn(user);

        UserModel result = userService.create(user);

        assertNotNull(result);
        verify(userRepository).save(user);
    }

    // PROFILE & UPDATE TESTS

    @Test
    void givenAuthenticatedUserWithUniqueNewEmail_whenUpdateProfile_shouldSaveAndReturnResponseWithToken() {
        String oldEmail = "old@mail.com";
        String newEmail = "new@mail.com";
        mockSecurityContext(oldEmail);

        UserDTO.UpdateProfileRequest request = new UserDTO.UpdateProfileRequest(newEmail, "123456789");

        UserModel user = new UserModel();
        user.setEmail(oldEmail);
        user.setRole(RoleEnum.ROLE_CUSTOMER);

        UserModel savedUser = new UserModel();
        savedUser.setEmail(newEmail);
        savedUser.setPhoneNumber("123456789");
        savedUser.setRole(RoleEnum.ROLE_CUSTOMER);

        when(userRepository.findByEmail(oldEmail)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(newEmail)).thenReturn(false);
        when(userRepository.save(any(UserModel.class))).thenReturn(savedUser);
        when(jwtService.generateToken(newEmail, "ROLE_CUSTOMER")).thenReturn("newToken");
        when(accountRepository.findAccounts(eq(newEmail), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        UserDTO.UpdateProfileResponse response = userService.updateProfile(request);

        assertNotNull(response);
        assertEquals(newEmail, response.email());
        assertEquals("newToken", response.token());
        verify(userRepository).save(any(UserModel.class));
    }

    @Test
    void givenEmailOwnedByAnotherUser_whenUpdateProfile_shouldThrowIllegalStateException() {
        String oldEmail = "old@mail.com";
        String takenEmail = "taken@mail.com";
        mockSecurityContext(oldEmail);

        UserDTO.UpdateProfileRequest request = new UserDTO.UpdateProfileRequest(takenEmail, "123456789");

        UserModel user = new UserModel();
        user.setEmail(oldEmail);

        when(userRepository.findByEmail(oldEmail)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail(takenEmail)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> userService.updateProfile(request));
        verify(userRepository, never()).save(any());
    }

    // APPROVAL & REGISTRATION WORKFLOW TESTS
    

    @Test
    void givenPendingUser_whenApproveUser_shouldProvisionProfileAndAccounts() {
        
        Long userId = 1L;
        UserModel user = new UserModel();
        user.setId(userId);
        user.setEmail("pending@mail.com");
        user.setIsApproved(false);

        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);

        AccountModel checkingAccount = new AccountModel();
        checkingAccount.setIban("NL01INHOChecking");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerProfileRepository.findByUserEmail("pending@mail.com")).thenReturn(Optional.empty());
        when(customerProfileRepository.save(any(CustomerProfileModel.class))).thenReturn(profile);
        when(ibanGenerator.generateDutchIban()).thenReturn("NL01INHOChecking").thenReturn("NL01INHOSavings");
        when(accountRepository.save(any(AccountModel.class))).thenReturn(checkingAccount);

        userService.approveUser(userId);


        assertTrue(user.getIsApproved());
        assertEquals("NL01INHOChecking", user.getIban());
        verify(customerProfileRepository).save(any(CustomerProfileModel.class));
        verify(accountRepository, times(2)).save(any(AccountModel.class));
        verify(userRepository).save(user);
    }

    @Test
    void givenAlreadyApprovedUser_whenApproveUser_shouldReturnEarlyWithoutProcessing() {
        // Arrange
        Long userId = 1L;
        UserModel user = new UserModel();
        user.setIsApproved(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.approveUser(userId);

        verify(customerProfileRepository, never()).findByUserEmail(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void givenExistingUser_whenDenyUser_shouldDeleteRecordFromDatabase() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        userService.denyUser(userId);

        verify(userRepository).deleteById(userId);
    }

    @Test
    void givenNonExistingUser_whenDenyUser_shouldThrowEntityNotFoundException() {
        Long userId = 99L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> userService.denyUser(userId));
        verify(userRepository, never()).deleteById(userId);
    }
}