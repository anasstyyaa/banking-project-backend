package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.models.AccountTypeEnum;
import inholland.nl.banking_project_backend.models.TransactionTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public class AtmDTO {
    public record MoneyRequest(
            @NotNull Long accountId,
            @NotNull @DecimalMin("0.01") BigDecimal amount
    ) {}

    public record AccountResponse(
            Long id,
            String iban,
            AccountTypeEnum type,
            BigDecimal balance,
            BigDecimal absoluteLimit,
            BigDecimal dailyLimit
    ) {}

    public record TransactionResponse(
            Long id,
            Long accountId,
            String iban,
            TransactionTypeEnum type,
            BigDecimal amount,
            BigDecimal balance,
            Instant timestamp
    ) {}
}
