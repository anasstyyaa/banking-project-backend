package inholland.nl.banking_project_backend.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionFilterRequestDTO(
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal amountLessThan,
        BigDecimal amountGreaterThan,
        BigDecimal amountEqualTo,
        String iban,
        Long userId
) {}
