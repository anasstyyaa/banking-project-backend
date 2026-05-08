package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserModel create(UserModel user) {
        return userRepository.save(user);
    }

    public UserModel findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}
