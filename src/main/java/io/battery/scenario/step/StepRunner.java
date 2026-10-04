package io.battery.scenario.step;

import java.util.List;
import java.util.Map;

import io.battery.scenario.CancellationMarker;
import io.battery.model.Step;

/**
 * Runs a sequence of steps, passing the state returned by each step to the next and
 * stopping with a {@link io.battery.scenario.ScenarioCancellationException} on cancellation.
 */
@FunctionalInterface
public interface StepRunner {
    Map<String, Object> runSteps(List<Step> steps, Map<String, Object> initialState,
                                 CancellationMarker cancellationMarker);
}
