package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.UserSerivce;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserSerivce userService;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;

    public UserDTO.LoginResponse register(UserDTO.RegisterRequest dto) {
        UserModel newUser = new UserModel();
        newUser.setEmail(dto.email());
        newUser.setPassword(dto.password());
        newUser.setBsn(dto.bsn());
        newUser.setFirstName(dto.firstName());
        newUser.setLastName(dto.lastName());
        newUser.setPhoneNumber(dto.phoneNumber());
        newUser.setRole(RoleEnum.ROLE_CUSTOMER);
        newUser.setIsApproved(false);

        UserModel savedUser = userService.create(newUser);
        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new UserDTO.LoginResponse(
                savedUser.getEmail(),
                token,
                savedUser.getRole()
        );
    }

    public UserDTO.LoginResponse login(UserDTO.LoginRequest dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        );

        UserModel user = userService.findByEmail(dto.email());
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        return new UserDTO.LoginResponse(
                user.getEmail(),
                token,
                user.getRole()
        );
    }

}
