package inholland.nl.banking_project_backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionRequestValidator.class)
public @interface ValidTransactionRequest {
    String message() default "Transaction request contains missing account details.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
