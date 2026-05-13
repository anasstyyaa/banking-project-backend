package inholland.nl.banking_project_backend.services.impl;

import inholland.nl.banking_project_backend.exceptions.UserAlreadyExistsException;
import inholland.nl.banking_project_backend.models.CustomerProfileModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.CustomerProfileRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import inholland.nl.banking_project_backend.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    // Creates a new customer user with encoded password and empty profile.
    @Override
    public UserModel createCustomer(UserModel user) {
        validateEmailIsAvailable(user.getEmail());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        UserModel savedUser = userRepository.save(user);
        createProfile(savedUser);
        return savedUser;
    }

    // Finds a user by email or throws a business-friendly error.
    @Override
    public UserModel findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    // Prevents duplicate users from registering with the same email.
    private void validateEmailIsAvailable(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("A user with this email already exists.");
        }
    }

    // Creates the customer banking profile linked to the saved user.
    private void createProfile(UserModel user) {
        CustomerProfileModel profile = new CustomerProfileModel();
        profile.setUser(user);
        customerProfileRepository.save(profile);
    }
}
