package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AccountDTO {
    public record AccountResponse(
            String iban,
            AccountTypeEnum type,
            BigDecimal balance,
            Boolean isActive,
            BigDecimal absoluteLimit,
            BigDecimal dailyLimit,
            String customerEmail,
            String customerName
    ) {}

    public record AccountCreationRequest(
            @NotNull AccountTypeEnum accountType,
            @NotNull BigDecimal absoluteLimit,
            @NotNull @DecimalMin("0.01") BigDecimal dailyLimit
    ) {}

    public record UpdateLimitsRequest(
            @NotNull BigDecimal absoluteLimit,
            @NotNull @DecimalMin("0.01") BigDecimal dailyLimit
    ) {}

    public record AccountSearchResponse(
            String iban,
            AccountTypeEnum type,
            String customerName
    ) {}
}
