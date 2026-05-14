package inholland.nl.banking_project_backend.mappers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.models.RoleEnum;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserModel toEntity(UserDTO.RegisterRequest dto) {
        if (dto == null) return null;

        UserModel user = new UserModel();
        user.setEmail(dto.email());
        user.setBsn(dto.bsn());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhoneNumber(dto.phoneNumber());
        user.setRole(RoleEnum.ROLE_CUSTOMER);
        user.setIsApproved(false);

        return user;
    }

    public UserDTO.UserResponse toResponse(UserModel entity) {
        if (entity == null) return null;

        return new UserDTO.UserResponse(
                entity.getFirstName(),
                entity.getLastName(),
                entity.getIban(),
                entity.getEmail(),
                entity.getRole()
        );
    }

    public UserDTO.RegistrationRequest toRegistrationRequest(UserModel entity) {
        if (entity == null) return null;

        return new UserDTO.RegistrationRequest(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getBsn(),
                entity.getPhoneNumber(),
                entity.getRole()
        );
    }
}