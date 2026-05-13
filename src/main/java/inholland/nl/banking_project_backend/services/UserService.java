package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.utils.IbanGenerator;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final IbanGenerator ibanGenerator;
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

    public List<UserModel> getPendingUsers() {
        return userRepository.findAllByIsApprovedFalse();
    }

    @Transactional
    public void approveUser(Long userId) {
        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (user.getIsApproved()) {
            return;
        }

        if (user.getIban() == null || user.getIban().isEmpty()) {
            user.setIban(ibanGenerator.generateDutchIban());
        }

        user.setIsApproved(true);
        userRepository.save(user);
    }

    public void denyUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
    }
}
