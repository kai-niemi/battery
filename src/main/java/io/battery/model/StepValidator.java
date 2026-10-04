package io.battery.model;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link StepValidation}, reporting each violation on the offending property.
 */
public class StepValidator implements ConstraintValidator<StepValidation, Step> {
    @Override
    public boolean isValid(Step step, ConstraintValidatorContext context) {
        boolean isValid = true;

        if (Objects.isNull(step.getSql()) && Objects.isNull(step.getScript()) && Objects.isNull(step.getPath())) {
            context.buildConstraintViolationWithTemplate(
                            "one of sql, script or path is required")
                    .addConstraintViolation();
            isValid = false;
        }

        if (Objects.nonNull(step.getSql()) && (Objects.nonNull(step.getScript()) || Objects.nonNull(step.getPath()))) {
            context.buildConstraintViolationWithTemplate(
                            "sql and script|path are mutually exclusive")
                    .addPropertyNode("sql")
                    .addConstraintViolation();
            isValid = false;
        }

        if (Objects.nonNull(step.getScript()) && (Objects.nonNull(step.getSql()) || Objects.nonNull(step.getPath()))) {
            context.buildConstraintViolationWithTemplate(
                            "script and sql|path are mutually exclusive")
                    .addPropertyNode("script")
                    .addConstraintViolation();
            isValid = false;
        }

        if (Objects.nonNull(step.getPath()) && (Objects.nonNull(step.getSql()) || Objects.nonNull(step.getScript()))) {
            context.buildConstraintViolationWithTemplate(
                            "path and sql|script are mutually exclusive")
                    .addPropertyNode("path")
                    .addConstraintViolation();
            isValid = false;
        }


        if (Objects.nonNull(step.getPath()) && ScriptFormatUtils.resolveFormat(step.getPath()).isEmpty()) {
            context.buildConstraintViolationWithTemplate(
                            "unable to resolve script file format")
                    .addPropertyNode("path")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
