package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;

import java.math.BigDecimal;

public record AccountResponseDTO(
        String iban,
        AccountTypeEnum type,
        BigDecimal balance,
        Boolean isActive,
        BigDecimal absoluteLimit,
        BigDecimal dailyLimit,
        String customerEmail,
        String customerName
) {}
