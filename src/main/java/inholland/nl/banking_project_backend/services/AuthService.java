package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UserDTO.LoginResponse register(UserDTO.RegisterRequest dto) {
        if (userService.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserModel newUser = userMapper.toEntity(dto);
        newUser.setPassword(passwordEncoder.encode(dto.password()));

        UserModel savedUser = userService.create(newUser);

        String token = jwtService.generateToken(
                savedUser.getEmail(),
                savedUser.getRole().name()
        );

        return new UserDTO.LoginResponse(
                savedUser.getEmail(),
                token,
                savedUser.getRole()
        );
    }

    public UserDTO.LoginResponse login(UserDTO.LoginRequest dto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        dto.email(),
                        dto.password()
                )
        );

        UserModel user = userService.findByEmail(dto.email());

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new UserDTO.LoginResponse(
                user.getEmail(),
                token,
                user.getRole()
        );
    }
}