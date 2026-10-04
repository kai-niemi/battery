package io.battery.scenario;

/**
 * Lifecycle status of the current or most recent scenario launch.
 *
 * @see ScenarioListener
 */
public enum ScenarioStatus {
    IDLE,
    RUNNING,
    CANCELLING,
    CANCELLED,
    COMPLETED,
    FAILED,
}
