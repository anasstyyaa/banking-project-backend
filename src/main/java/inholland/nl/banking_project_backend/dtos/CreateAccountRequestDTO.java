package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequestDTO(
        @NotNull AccountTypeEnum accountType,
        @NotNull BigDecimal absoluteLimit,
        @NotNull @DecimalMin("0.01") BigDecimal dailyLimit
) {}
