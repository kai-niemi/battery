package io.battery.scenario;

import io.battery.model.Phase;
import io.battery.model.ScenarioException;
import io.battery.model.Step;

/**
 * Thrown when a phase or step is stopped because the scenario launch was cancelled.
 *
 * @see CancellationMarker
 */
public class ScenarioCancellationException extends ScenarioException {
    public ScenarioCancellationException(Step step, CancellationMarker cancellationMarker) {
        super("Cancelling step [%s] due to [%s]".formatted(step.getName(), cancellationMarker.getReason()));
    }

    public ScenarioCancellationException(Phase phase, CancellationMarker cancellationMarker) {
        super("Cancelling phase [%s] due to [%s]".formatted(phase.getName(), cancellationMarker.getReason()));
    }
}
