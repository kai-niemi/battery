package io.battery.model;

/**
 * Thrown when a virtual user hits a non-recoverable error while running a scenario, or a
 * transient error when the scenario doesn't continue on transient errors.
 */
public class ScenarioExecutionException extends ScenarioException {
    public ScenarioExecutionException(Scenario scenario, Throwable cause) {
        super("Non-recoverable execution error in [%s]".formatted(scenario.getName()), cause);
    }
}
