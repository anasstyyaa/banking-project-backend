package inholland.nl.banking_project_backend.validation;

import inholland.nl.banking_project_backend.dtos.CreateTransactionRequestDTO;
import inholland.nl.banking_project_backend.enums.TransactionTypeEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, CreateTransactionRequestDTO> {

    // Validates transaction-specific account fields after basic DTO annotations run.
    @Override
    public boolean isValid(CreateTransactionRequestDTO request, ConstraintValidatorContext context) {
        if (request == null || request.type() == null) {
            return true;
        }

        return switch (request.type()) {
            case TRANSFER -> validateRequiredFields(
                    context,
                    hasValue(request.fromIban()) && hasValue(request.toIban()),
                    "Transfer requires both source and destination IBAN."
            );
            case DEPOSIT -> validateRequiredFields(
                    context,
                    hasValue(request.toIban()),
                    "Deposit requires a destination IBAN."
            );
            case WITHDRAWAL -> validateRequiredFields(
                    context,
                    hasValue(request.fromIban()),
                    "Withdrawal requires a source IBAN."
            );
        };
    }

    // Adds a clear validation message when transaction-specific fields are missing.
    private boolean validateRequiredFields(ConstraintValidatorContext context, boolean valid, String message) {
        if (valid) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    // Checks whether an optional IBAN field was provided.
    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
