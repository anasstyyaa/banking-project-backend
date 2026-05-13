package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TransactionDTO {
    public record CreateRequest(
            @NotNull TransactionTypeEnum type,
            String fromIban,
            String toIban,
            @NotNull @DecimalMin(value = "0.01") BigDecimal amount
    ) {}

    public record FilterRequest(
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amountLessThan,
            BigDecimal amountGreaterThan,
            BigDecimal amountEqualTo,
            String iban
    ) {}

    public record TransactionResponse(
            Long id,
            TransactionTypeEnum type,
            String fromIban,
            String toIban,
            BigDecimal amount,
            LocalDateTime timestamp,
            String initiatedByEmail
    ) {}
}
