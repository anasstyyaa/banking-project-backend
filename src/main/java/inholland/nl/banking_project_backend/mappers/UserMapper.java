package inholland.nl.banking_project_backend.mappers;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.UserModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", constant = "ROLE_CUSTOMER")
    @Mapping(target = "isApproved", constant = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "iban", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    UserModel toEntity(UserDTO.RegisterRequest dto);


    UserDTO.UserResponse toResponse(UserModel entity);
    UserDTO.RegistrationRequest toRegistrationRequest(UserModel entity);
}
