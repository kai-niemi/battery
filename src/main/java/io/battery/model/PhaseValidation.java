package io.battery.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a {@link Phase} uses either {@code users} or a creation rate but not both,
 * and that {@code startRate} doesn't exceed {@code maxRate}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhaseValidator.class)
public @interface PhaseValidation {
    String message() default "Invalid phase constraints";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
