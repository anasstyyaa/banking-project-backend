package inholland.nl.banking_project_backend.dtos;

import java.time.LocalDateTime;

public record ApiErrorDTO(
        String message,
        LocalDateTime timestamp
) {}
