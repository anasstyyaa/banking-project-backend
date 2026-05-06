package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserSerivce {
    private final UserRepository userRepository;
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
=======

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
//mappear
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
>>>>>>> parent of 57be30c (Revert "..")
}
