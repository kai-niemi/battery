package io.battery.scenario.step;

import io.battery.model.Step;

/**
 * Action performing a step of a given kind, such as inline SQL or a script file.
 *
 * @param <T> the state type passed between steps
 */
@FunctionalInterface
public interface StepAction<T> {
    default boolean accepts(Step step) {
        return false;
    }

    T perform(Step step, T initialState);
}
