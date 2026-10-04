package io.battery.model;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link PhaseValidation}, reporting each violation on the offending property.
 */
public class PhaseValidator implements ConstraintValidator<PhaseValidation, Phase> {
    @Override
    public boolean isValid(Phase phase, ConstraintValidatorContext context) {
        boolean isValid = true;

        // Report the specific violations below instead of the generic default message
        context.disableDefaultConstraintViolation();

        if (phase.getUsers() > 0 && (phase.getStartRate() > 0 || phase.getMaxRate() > 0)) {
            context.buildConstraintViolationWithTemplate(
                            "users can't be combined with startRate or maxRate")
                    .addPropertyNode("users")
                    .addConstraintViolation();
            isValid = false;
        }

        if (phase.getMaxRate() > 0 && phase.getStartRate() > phase.getMaxRate()) {
            context.buildConstraintViolationWithTemplate(
                            "startRate must be <= maxRate")
                    .addPropertyNode("startRate")
                    .addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }
}
