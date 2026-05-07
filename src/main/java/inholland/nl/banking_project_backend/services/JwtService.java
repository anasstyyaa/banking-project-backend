package inholland.nl.banking_project_backend.services;

public interface JwtService {
    // Generates a signed JWT for the supplied email and role.
    String generateToken(String email, String role);

    // Extracts the email subject from a JWT.
    String extractEmail(String token);

    // Extracts the role claim from a JWT.
    String extractRole(String token);

    // Checks whether a JWT belongs to the supplied email and is not expired.
    boolean isTokenValid(String token, String email);
}
