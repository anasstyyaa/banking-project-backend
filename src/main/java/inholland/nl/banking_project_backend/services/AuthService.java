package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;
import inholland.nl.banking_project_backend.exceptions.UserAlreadyExistsException;
import inholland.nl.banking_project_backend.mappers.UserMapper;
import inholland.nl.banking_project_backend.models.UserModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
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
            throw new UserAlreadyExistsException("Email already exists");
        }

        if (!isValidBSN(dto.bsn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "bsn: The provided BSN is mathematically invalid");
        }

        UserModel newUser = userMapper.toEntity(dto);
        newUser.setPassword(passwordEncoder.encode(dto.password()));

        UserModel savedUser = userService.create(newUser);
        log.info("New registration request submitted for email: {}. Status: PENDING", savedUser.getEmail());

        String token = jwtService.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new UserDTO.LoginResponse(
                savedUser.getEmail(),
                token,
                savedUser.getRole(),
                savedUser.getIsApproved()
        );
    }

    public UserDTO.LoginResponse login(UserDTO.LoginRequest dto) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.email(),
                            dto.password()
                    )
            );
        } catch (DisabledException e) {
            log.warn("Login blocked: Account '{}' is pending employee approval.", dto.email());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCOUNT_PENDING_APPROVAL");
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        UserModel user = userService.findByEmail(dto.email());

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new UserDTO.LoginResponse(
                user.getEmail(),
                token,
                user.getRole(),
                user.getIsApproved()
        );
    }

    private boolean isValidBSN(String bsn) {
        if (bsn == null || bsn.length() != 9) return false;

        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(bsn.charAt(i)) * (9 - i);
        }
        sum -= Character.getNumericValue(bsn.charAt(8));

        return sum % 11 == 0;
    }
}
