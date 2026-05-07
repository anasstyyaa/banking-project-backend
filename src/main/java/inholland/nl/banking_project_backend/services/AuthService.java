package inholland.nl.banking_project_backend.services;

import inholland.nl.banking_project_backend.dtos.UserDTO;

public interface AuthService {
    // Registers a new customer and returns a JWT login response.
    UserDTO.LoginResponse register(UserDTO.RegisterRequest dto);

    // Authenticates an existing user and returns a JWT login response.
    UserDTO.LoginResponse login(UserDTO.LoginRequest dto);
}
