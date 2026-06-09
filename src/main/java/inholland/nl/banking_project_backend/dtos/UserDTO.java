package inholland.nl.banking_project_backend.dtos;
import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import inholland.nl.banking_project_backend.enums.RegistrationDecisionEnum;
import inholland.nl.banking_project_backend.models.RoleEnum;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public class UserDTO {
    public record RegisterRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Please provide a valid email address structure")
            @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$", message = "Email format is invalid")
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters long")
            @Pattern(
                    regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_]).*$",
                    message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@#$%^&+=!_)"
            )
            String password,

            @NotBlank(message = "BSN number is required")
            @Pattern(regexp = "^[0-9]{9}$", message = "BSN must be exactly 9 digits long")
            String bsn,

            @NotBlank(message = "First name is required")
            @Size(max = 50, message = "First name cannot exceed 50 characters")
            String firstName,

            @NotBlank(message = "Last name is required")
            @Size(max = 50, message = "Last name cannot exceed 50 characters")
            String lastName,

            @NotBlank(message = "Phone number is required")
            @Pattern(
                    regexp = "^(\\+31|0|0031)[1-9][0-9]{8}$",
                    message = "Please provide a valid Dutch mobile or landline phone number (e.g., +31612345678 or 0612345678)"
            )
            String phoneNumber
    ) {}

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record LoginResponse(
            String email,
            String token,
            RoleEnum role,
            Boolean isApproved
    ) {}

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
            String iban,
            Boolean isApproved,
            RoleEnum role
    ) {}

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int number,
            int size
    ) {}

    public record UpdateRegistrationStatusRequest(
            @NotNull RegistrationDecisionEnum status,
            BigDecimal absoluteLimit,
            @DecimalMin("0.01") BigDecimal dailyLimit
    ) {}

    public record ProfileResponse(
            String email,
            String firstName,
            String lastName,
            String iban,
            String bsn,
            String phoneNumber,
            BigDecimal totalBalance,
            List<AccountDetailsResponse> accounts
    ) {}

    public record UpdateProfileResponse(
            String email,
            String firstName,
            String lastName,
            String iban,
            String bsn,
            String phoneNumber,
            BigDecimal totalBalance,
            List<AccountDetailsResponse> accounts,
            String token,
            RoleEnum role
    ) {}

    public record AccountDetailsResponse(
            Long id,
            String iban,
            AccountTypeEnum type,
            BigDecimal balance
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Email String email,
            @NotBlank String phoneNumber
    ) {}
}
