package io.battery.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates that a {@link Step} sets exactly one of {@code sql}, {@code script} and
 * {@code path}, and that a path has a known {@link ScriptFormat}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StepValidator.class)
public @interface StepValidation {
    String message() default "Invalid step constraints";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
