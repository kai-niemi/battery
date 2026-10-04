package io.battery.scenario.worker;

/**
 * Callback interface for a single unit-of-work, typically running through all steps of
 * a scenario using an explicit transaction or set of implicit transactions.
 */
@FunctionalInterface
public interface UnitOfWork {
    /**
     * Single work unit.
     *
     * @param iteration the total number of iterations for this work unit including retries. Starts at 1.
     */
    void call(int iteration);
}
