package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import inholland.nl.banking_project_backend.utils.IbanGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final IbanGenerator ibanGenerator;
    private final JWTService jwtService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public UserModel create(UserModel user) {
        return userRepository.save(user);
    }

    public UserModel findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    public UserDTO.ProfileResponse getProfile(String email) {
        return toProfileResponse(findByEmail(email));
    }

    // Updates the profile for the currently authenticated user and returns a fresh token if email changed.
    public UserDTO.UpdateProfileResponse updateProfile(UserDTO.UpdateProfileRequest request) {
        String email = getAuthEmail();
        UserModel user = findByEmail(email);

        if (!user.getEmail().equals(request.email()) && userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("This email is already used by another account.");
        }

        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());

        UserModel savedUser = userRepository.save(user);
        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());
        return toUpdateProfileResponse(savedUser, token);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<UserModel> getPendingUsers() {
        return userRepository.findAllByIsApprovedFalse();
    }

    public List<UserModel> getActiveUsers() {
        return userRepository.findAllByIsApprovedTrue();
    }

    @Transactional(rollbackOn = Exception.class)
    public void approveUser(Long userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getIsApproved()) return;
        user.setIsApproved(true);

        CustomerProfileModel profile = customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> {
                    CustomerProfileModel newProfile = new CustomerProfileModel();
                    newProfile.setUser(user);
                    return customerProfileRepository.save(newProfile);
                });

        AccountModel checking = createAccount(profile, AccountTypeEnum.CHECKING, new BigDecimal("1000.00"));
        createAccount(profile, AccountTypeEnum.SAVINGS, BigDecimal.ZERO);
        user.setIban(checking.getIban());
        userRepository.save(user);

        log.info("Successfully provisioned checking/savings portfolio for User ID: {}", userId);
    }//test 

    public void denyUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

        log.info("Employee denied registration for User ID: {}. Removing record.", userId);
        userRepository.deleteById(userId);
    }

    private CustomerProfileModel createCustomerProfile(UserModel user) {
        return customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> saveCustomerProfile(user));
    }
    
    private CustomerProfileModel saveCustomerProfile(UserModel user) {
        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);
        return customerProfileRepository.save(profile);
    }

    // Creates a customer account with default transaction limits
    private AccountModel createAccount(CustomerProfileModel profile, AccountTypeEnum type, BigDecimal balance) {
        AccountModel account = new AccountModel();
        account.setCustomer(profile);
        account.setIban(ibanGenerator.generateDutchIban());
        account.setType(type);
        account.setBalance(balance);
        account.setAbsoluteLimit(new BigDecimal("-500.00"));
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setIsActive(true);
        return accountRepository.save(account);
    }

    // Maps the authenticated customer, their accounts into the profile
    private UserDTO.ProfileResponse toProfileResponse(UserModel user) {
        List<AccountModel> accounts = getAccountsForProfile(user);
        BigDecimal totalBalance = accounts.stream()
                .map(AccountModel::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserDTO.ProfileResponse(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getIban(),
                user.getBsn(),
                user.getPhoneNumber(),
                totalBalance,
                accounts.stream().map(this::toAccountDetailsResponse).toList()
        );
    }

    // Maps the updated customer profile and embeds a fresh token for the new email identity.
    private UserDTO.UpdateProfileResponse toUpdateProfileResponse(UserModel user, String token) {
        List<AccountModel> accounts = getAccountsForProfile(user);
        BigDecimal totalBalance = accounts.stream()
                .map(AccountModel::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserDTO.UpdateProfileResponse(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getIban(),
                user.getBsn(),
                user.getPhoneNumber(),
                totalBalance,
                accounts.stream().map(this::toAccountDetailsResponse).toList(),
                token,
                user.getRole()
        );
    }

    private List<AccountModel> getAccountsForProfile(UserModel user) {
        return accountRepository.findAccounts(user.getEmail(), Pageable.unpaged()).getContent();
    }

    // Keeps account mapping limited to fields the customer may view.
    private UserDTO.AccountDetailsResponse toAccountDetailsResponse(AccountModel account) {
        return new UserDTO.AccountDetailsResponse(
                account.getId(),
                account.getIban(),
                account.getType(),
                account.getBalance()
        );
    }

    private String getAuthEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
