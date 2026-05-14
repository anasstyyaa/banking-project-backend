package inholland.nl.banking_project_backend.dtos;
import inholland.nl.banking_project_backend.models.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String bsn,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String phoneNumber
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record LoginResponse(
            String email,
            String token,
            RoleEnum role
    ) {}

    // for showing user info safely
    public record UserResponse(
            String firstName,
            String lastName,
            String iban,
            String email,
            RoleEnum role
    ) {}

    public record RegistrationRequest(
            Long id,
            String firstName,
            String lastName,
            String email,
            String bsn,
            String phoneNumber,
            RoleEnum role
    ) {}
}
