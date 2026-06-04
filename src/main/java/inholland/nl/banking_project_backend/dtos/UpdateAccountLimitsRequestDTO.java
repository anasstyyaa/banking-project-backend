package inholland.nl.banking_project_backend.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateAccountLimitsRequestDTO(
        @NotNull BigDecimal absoluteLimit,
        @NotNull @DecimalMin("0.01") BigDecimal dailyLimit
) {}
