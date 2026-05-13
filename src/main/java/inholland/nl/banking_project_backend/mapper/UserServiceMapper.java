package inholland.nl.banking_project_backend.mapper;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.enums.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import org.springframework.stereotype.Component;

@Component
public class UserServiceMapper {
    // Converts a registration request into a new customer user entity.
    public UserModel toCustomerModel(UserDTO.RegisterRequest dto) {
        UserModel user = new UserModel();
        user.setEmail(dto.email());
        user.setPassword(dto.password());
        user.setBsn(dto.bsn());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setRole(RoleEnum.ROLE_CUSTOMER);
        user.setIsApproved(false);
        return user;
    }

    // Converts an authenticated user and token into the login response DTO.
    public UserDTO.LoginResponse toLoginResponse(UserModel user, String token) {
        return new UserDTO.LoginResponse(user.getEmail(), token, user.getRole());
    }
}
