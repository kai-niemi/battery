package io.battery.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

@Tag("unit-test")
public class BatteryModelTest {
    private static Step step(String name) {
        Step step = new Step();
        step.setName(name);
        step.setSql("select 1");
        return step;
    }

    private static Scenario scenario(String name, Step... steps) {
        Scenario scenario = new Scenario();
        scenario.setName(name);
        scenario.setSteps(List.of(steps));
        return scenario;
    }

    @Test
    public void givenUniqueStepNames_expectValid() {
        BatteryModel batteryModel = new BatteryModel();
        batteryModel.getBefore().setSteps(List.of(step("prepare")));
        batteryModel.setScenarios(List.of(
                scenario("one", step("insert one")),
                scenario("two", step("insert two"))));

        batteryModel.init();
    }

    @Test
    public void givenNoPrimaryScenario_expectFirstScenarioPrimary() {
        BatteryModel batteryModel = new BatteryModel();
        batteryModel.setScenarios(List.of(
                scenario("one", step("insert one")),
                scenario("two", step("insert two"))));

        batteryModel.init();

        Assertions.assertThat(batteryModel.getPrimaryScenario())
                .map(Scenario::getName)
                .hasValue("one");
    }

    @Test
    public void givenFirstScenarioMarkedPrimary_expectItStaysPrimary() {
        BatteryModel batteryModel = new BatteryModel();
        Scenario one = scenario("one", step("insert one"));
        one.setPrimary(true);
        batteryModel.setScenarios(List.of(one, scenario("two", step("insert two"))));

        batteryModel.init();

        Assertions.assertThat(batteryModel.getPrimaryScenario())
                .map(Scenario::getName)
                .hasValue("one");
    }

    @Test
    public void givenOtherScenarioMarkedPrimary_expectOnlyItPrimary() {
        BatteryModel batteryModel = new BatteryModel();
        Scenario two = scenario("two", step("insert two"));
        two.setPrimary(true);
        batteryModel.setScenarios(List.of(scenario("one", step("insert one")), two));

        batteryModel.init();

        Assertions.assertThat(batteryModel.getScenarios())
                .filteredOn(Scenario::isPrimary)
                .extracting(Scenario::getName)
                .containsExactly("two");
    }

    @Test
    public void givenNoScenarios_expectNoPrimaryScenario() {
        BatteryModel batteryModel = new BatteryModel();

        batteryModel.init();

        Assertions.assertThat(batteryModel.getPrimaryScenario()).isEmpty();
    }

    @Test
    public void givenReadOnlyScriptFile_expectPathResolved(@TempDir Path baseDir) throws IOException {
        Path file = Files.writeString(baseDir.resolve("select.sql"), "select 1;");
        Assumptions.assumeTrue(file.toFile().setWritable(false) && !Files.isWritable(file),
                "file system doesn't support read-only files");

        Step step = new Step();
        step.setName("select");
        step.setPath(Path.of("select.sql"));
        step.resolvePath(baseDir);

        Assertions.assertThat(step.getPath()).isEqualTo(file);
    }

    @Test
    public void givenMissingScriptFile_expectModelException(@TempDir Path baseDir) {
        Step step = new Step();
        step.setName("select");
        step.setPath(Path.of("missing.sql"));

        Assertions.assertThatThrownBy(() -> step.resolvePath(baseDir))
                .isInstanceOf(ModelException.class)
                .hasMessageStartingWith("Invalid path: ");
    }

    @Test
    public void givenStepWithoutSqlScriptOrPath_expectViolation() {
        Step step = new Step();
        step.setName("empty");

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Set<ConstraintViolation<Step>> violations = factory.getValidator().validate(step);

            Assertions.assertThat(violations)
                    .extracting(ConstraintViolation::getMessage)
                    .contains("one of sql, script or path is required");
        }
    }

    @Test
    public void givenStepWithSql_expectNoViolations() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Assertions.assertThat(factory.getValidator().validate(step("select"))).isEmpty();
        }
    }
}
