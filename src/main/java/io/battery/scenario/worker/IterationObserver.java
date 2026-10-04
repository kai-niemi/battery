package io.battery.scenario.worker;

import java.time.Duration;

/**
 * Observes each iteration of a virtual user's scenario steps, for example to record a run
 * summary. Called on the virtual user's own thread, so implementations must be thread-safe.
 */
@FunctionalInterface
public interface IterationObserver {
    enum Outcome {
        SUCCESS,
        // Retried, if the scenario continues on transient errors
        TRANSIENT_ERROR,
        // Ends the virtual user
        ERROR
    }

    /**
     * @param duration the iteration time, including any time waiting for a connection
     * @param outcome  the outcome of the iteration
     * @param cause    the most specific cause of an error, or null on success
     */
    void onIteration(Duration duration, Outcome outcome, Throwable cause);
}
