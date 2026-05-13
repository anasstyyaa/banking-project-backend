package inholland.nl.banking_project_backend.services.impl;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.mapper.UserServiceMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import inholland.nl.banking_project_backend.services.AuthService;
import inholland.nl.banking_project_backend.services.JwtService;
import inholland.nl.banking_project_backend.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final JwtService jwtService;
    private final UserServiceMapper userServiceMapper;
    private final AuthenticationManager authenticationManager;

    // Registers a customer using shared user creation and JWT generation.
    @Override
    public UserDTO.LoginResponse register(UserDTO.RegisterRequest dto) {
        UserModel newUser = userServiceMapper.toCustomerModel(dto);
        UserModel savedUser = userService.createCustomer(newUser);
        return createLoginResponse(savedUser);
    }

    // Authenticates credentials through Spring Security before issuing a JWT.
    @Override
    public UserDTO.LoginResponse login(UserDTO.LoginRequest dto) {
        authenticate(dto);
        UserModel user = userService.findByEmail(dto.email());
        return createLoginResponse(user);
    }

    // Delegates credential verification to the authentication manager.
    private void authenticate(UserDTO.LoginRequest dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        );
    }

    // Creates a login response with a JWT containing the user's role.
    private UserDTO.LoginResponse createLoginResponse(UserModel user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return userServiceMapper.toLoginResponse(user, token);
    }
}
