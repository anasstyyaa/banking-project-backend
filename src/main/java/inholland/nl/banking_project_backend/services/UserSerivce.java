package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.AccountModel;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.repositories.AccountRepository;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;


@Service
@RequiredArgsConstructor
public class UserSerivce {
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserModel create(UserModel user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public UserModel findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));

    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
=======
>>>>>>> parent of ebea173 (Revert "qwerty")

   //profiles 
    public UserDTO.ProfileResponse getProfile(String email) {
        return toProfileResponse(findByEmail(email));
    }

    public UserDTO.ProfileResponse updatePhoneNumber(String email, UserDTO.UpdatePhoneNumberRequest request) {
        UserModel user = findByEmail(email);
        user.setPhoneNumber(request.phoneNumber());
        return toProfileResponse(userRepository.save(user));
    }

    private UserDTO.ProfileResponse toProfileResponse(UserModel user) {
        List<AccountModel> accounts = accountRepository.findByUserEmail(user.getEmail());
        BigDecimal totalBalance = accounts.stream()
                .map(AccountModel::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
<<<<<<< HEAD
//mappear
=======

>>>>>>> parent of ebea173 (Revert "qwerty")
        return new UserDTO.ProfileResponse(
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBsn(),
                user.getPhoneNumber(),
                totalBalance,
                accounts.stream().map(this::toAccountDetailsResponse).toList()
        );
    }

    private UserDTO.AccountDetailsResponse toAccountDetailsResponse(AccountModel account) {
        return new UserDTO.AccountDetailsResponse(
                account.getId(),
                account.getIban(),
                account.getType(),
                account.getBalance()
        );
    }
<<<<<<< HEAD
>>>>>>> parent of 57be30c (Revert "..")
=======
>>>>>>> parent of ebea173 (Revert "qwerty")
}
