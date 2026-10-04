package io.battery.model;

import java.time.Duration;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@Tag("unit-test")
public class PhaseTest {
    private static ValidatorFactory validatorFactory;

    private static Validator validator;

    @BeforeAll
    public static void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    public static void teardown() {
        validatorFactory.close();
    }

    private static Phase phase(int users, int startRate, int maxRate) {
        Phase phase = new Phase();
        phase.setName("test");
        phase.setDuration(Duration.ofSeconds(60));
        phase.setUsers(users);
        phase.setStartRate(startRate);
        phase.setMaxRate(maxRate);
        return phase;
    }

    @Test
    public void givenNoUsersOrRates_expectPausePhase() {
        Phase phase = phase(0, 0, 0);
        Assertions.assertThat(phase.isPausePhase()).isTrue();
        Assertions.assertThat(phase.isFixedPhase()).isFalse();
        Assertions.assertThat(phase.isRampingPhase()).isFalse();
        Assertions.assertThat(validator.validate(phase)).isEmpty();
    }

    @Test
    public void givenUsers_expectFixedPhase() {
        Phase phase = phase(20, 0, 0);
        Assertions.assertThat(phase.isFixedPhase()).isTrue();
        Assertions.assertThat(phase.isRampingPhase()).isFalse();
        Assertions.assertThat(phase.isPausePhase()).isFalse();
        Assertions.assertThat(validator.validate(phase)).isEmpty();
    }

    @Test
    public void givenStartRateOnly_expectConstantRate() {
        Phase phase = phase(0, 4, 0);
        Assertions.assertThat(phase.isRampingPhase()).isTrue();
        Assertions.assertThat(phase.getEffectiveMaxRate()).isEqualTo(4);
        Assertions.assertThat(validator.validate(phase)).isEmpty();
    }

    @Test
    public void givenMaxRateOnly_expectRampFromZero() {
        Phase phase = phase(0, 0, 10);
        Assertions.assertThat(phase.isRampingPhase()).isTrue();
        Assertions.assertThat(phase.getStartRate()).isZero();
        Assertions.assertThat(phase.getEffectiveMaxRate()).isEqualTo(10);
        Assertions.assertThat(validator.validate(phase)).isEmpty();
    }

    @Test
    public void givenStartAndMaxRate_expectLinearRamp() {
        Phase phase = phase(0, 4, 30);
        Assertions.assertThat(phase.isRampingPhase()).isTrue();
        Assertions.assertThat(phase.getEffectiveMaxRate()).isEqualTo(30);
        Assertions.assertThat(validator.validate(phase)).isEmpty();
    }

    @Test
    public void givenUsersAndRates_expectViolation() {
        Set<ConstraintViolation<Phase>> violations = validator.validate(phase(20, 4, 0));
        Assertions.assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("users can't be combined with startRate or maxRate");
    }

    @Test
    public void givenStartRateAboveMaxRate_expectViolation() {
        Set<ConstraintViolation<Phase>> violations = validator.validate(phase(0, 30, 4));
        Assertions.assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("startRate must be <= maxRate");
    }
}
