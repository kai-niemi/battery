package io.battery.model;

/**
 * Base class for errors that end a scenario launch or a virtual user, such as a failed step,
 * a non-recoverable error or a cancellation.
 */
public abstract class ScenarioException extends RuntimeException {
    public ScenarioException(String message) {
        super(message);
    }

    public ScenarioException(String message, Throwable cause) {
        super(message, cause);
    }
}
