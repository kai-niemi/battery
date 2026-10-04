package io.battery.scenario.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.model.Step;
import io.battery.model.StepExecutionException;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioCancellationException;

@Tag("unit-test")
public class SimpleStepRunnerTest {
    private static Step step(String name) {
        Step step = new Step();
        step.setName(name);
        return step;
    }

    /**
     * Step action provider resolving steps to actions by name without a bean factory.
     */
    private static StepActionProvider provider(Map<String, StepAction<Map<String, Object>>> actions) {
        return new StepActionProvider() {
            @Override
            public StepAction<Map<String, Object>> findAction(Step step) {
                return actions.get(step.getName());
            }
        };
    }

    private static StepAction<Map<String, Object>> put(String key) {
        return (step, state) -> {
            Map<String, Object> next = new HashMap<>(state);
            next.put(key, step.getName());
            return next;
        };
    }

    @Test
    public void givenSteps_expectStateChainedInSequence() {
        SimpleStepRunner runner = new SimpleStepRunner(
                provider(Map.of("first", put("a"), "second", put("b"))));

        Map<String, Object> result = runner.runSteps(List.of(step("first"), step("second")),
                Map.of("initial", true), new CancellationMarker());

        Assertions.assertThat(result).containsOnly(
                Map.entry("initial", true), Map.entry("a", "first"), Map.entry("b", "second"));
    }

    @Test
    public void givenFailingStep_expectFailureAttributedToThatStep() {
        StepAction<Map<String, Object>> failing = (step, state) -> {
            throw new IllegalStateException("boom");
        };
        SimpleStepRunner runner = new SimpleStepRunner(
                provider(Map.of("first", put("a"), "failing", failing, "last", put("c"))));

        Assertions.assertThatThrownBy(() -> runner.runSteps(
                        List.of(step("first"), step("failing"), step("last")), Map.of(), new CancellationMarker()))
                .isExactlyInstanceOf(StepExecutionException.class)
                .hasMessage("Step [failing] execution failed")
                .cause()
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
    }

    @Test
    public void givenCancellation_expectScenarioCancellationException() {
        CancellationMarker cancellationMarker = new CancellationMarker();
        cancellationMarker.markCancelled("test");

        SimpleStepRunner runner = new SimpleStepRunner(
                provider(Map.of("first", put("a"), "second", put("b"))));

        Assertions.assertThatThrownBy(() -> runner.runSteps(
                        List.of(step("first"), step("second")), Map.of(), cancellationMarker))
                .isExactlyInstanceOf(ScenarioCancellationException.class)
                .hasMessageContaining("Cancelling step [first]");
    }
}
