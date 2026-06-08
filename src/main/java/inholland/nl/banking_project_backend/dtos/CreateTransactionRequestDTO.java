package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateTransactionRequestDTO(
        @NotNull TransactionTypeEnum type,
        @Pattern(regexp = "^NL\\d{2}INHO\\d{10}$", message = "Source IBAN must use the NLxxINHOxxxxxxxxxx format")
        String fromIban,
        @Pattern(regexp = "^NL\\d{2}INHO\\d{10}$", message = "Destination IBAN must use the NLxxINHOxxxxxxxxxx format")
        String toIban,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
