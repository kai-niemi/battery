package io.battery.model;

/**
 * Thrown when a step fails, wrapping the cause.
 */
public class StepExecutionException extends ScenarioException {
    public StepExecutionException(Step step, Throwable cause) {
        super("Step [%s] execution failed".formatted(step.getName()), cause);
    }
}
