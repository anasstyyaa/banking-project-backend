package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long id,
        TransactionTypeEnum type,
        String fromIban,
        String toIban,
        BigDecimal amount,
        LocalDateTime timestamp,
        String initiatedByEmail
) {}
