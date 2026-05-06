package inholland.nl.banking_project_backend.dtos;
import inholland.nl.banking_project_backend.models.RoleEnum;
import inholland.nl.banking_project_backend.models.AccountTypeEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

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

    // showing user info 
    public record UserResponse(
            String firstName,
            String lastName,
            String iban,
            String email,
            RoleEnum role
    ) {}

    public record ProfileResponse(
            String email,
            String firstName,
            String lastName,
            String bsn,
            String phoneNumber,
            BigDecimal totalBalance,
            List<AccountDetailsResponse> accounts
    ) {}

    public record AccountDetailsResponse(
            Long id,
            String iban,
            AccountTypeEnum type,
            BigDecimal balance
    ) {}

    public record UpdatePhoneNumberRequest(
            @NotBlank String phoneNumber
    ) {}
}
