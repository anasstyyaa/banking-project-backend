package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.utils.IbanGenerator;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final AccountRepository accountRepository;
    private final IbanGenerator ibanGenerator;

    // Creates a user record after authentication-specific preparation is complete.
    public UserModel create(UserModel user) {
        return userRepository.save(user);
    }

    // Finds a user by email or throws a clean not-found exception.
    public UserModel findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

    }

    // Checks whether an email is already registered.
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // Returns users waiting for employee approval.
    public List<UserModel> getPendingUsers() {
        return userRepository.findAllByIsApprovedFalse();
    }

    // Approves a customer and creates their checking and savings accounts.
    @Transactional
    public void approveUser(Long userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getIsApproved()) {
            return;
        }

        user.setIsApproved(true);
        CustomerProfileModel profile = createCustomerProfile(user);
        AccountModel checkingAccount = createAccount(profile, AccountTypeEnum.CHECKING, new BigDecimal("1000.00"));
        createAccount(profile, AccountTypeEnum.SAVINGS, new BigDecimal("0.00"));
        user.setIban(checkingAccount.getIban());
        userRepository.save(user);
    }

    // Deletes a pending user after an employee denies registration.
    public void denyUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }

    // Creates or reuses the customer profile linked to a user.
    private CustomerProfileModel createCustomerProfile(UserModel user) {
        return customerProfileRepository.findByUserEmail(user.getEmail())
                .orElseGet(() -> saveCustomerProfile(user));
    }

    // Saves a customer profile for the approved user.
    private CustomerProfileModel saveCustomerProfile(UserModel user) {
        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);
        return customerProfileRepository.save(profile);
    }

    // Creates a customer account with default transaction limits.
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
}
