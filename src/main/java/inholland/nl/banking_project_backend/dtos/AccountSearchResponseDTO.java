package inholland.nl.banking_project_backend.dtos;

import inholland.nl.banking_project_backend.enums.AccountTypeEnum;

public record AccountSearchResponseDTO(
        String iban,
        AccountTypeEnum type,
        String customerName
) {}
