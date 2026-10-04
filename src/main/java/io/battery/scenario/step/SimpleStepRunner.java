package io.battery.scenario.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.battery.model.Step;
import io.battery.model.StepExecutionException;
import io.battery.scenario.CancellationMarker;
import io.battery.scenario.ScenarioCancellationException;

/**
 * Step runner executing steps in sequence on the calling thread, so that steps participate
 * in any thread-bound transaction, such as the explicit transaction of transactional scenarios.
 */
public class SimpleStepRunner implements StepRunner {
    private final StepActionProvider stepActionProvider;

    public SimpleStepRunner(StepActionProvider stepActionProvider) {
        this.stepActionProvider = stepActionProvider;
    }

    protected StepActionProvider getStepActionProvider() {
        return stepActionProvider;
    }

    @Override
    public Map<String, Object> runSteps(List<Step> steps, Map<String, Object> initialState,
                                        CancellationMarker cancellationMarker) {
        Map<String, Object> stepState = new HashMap<>(initialState);

        for (Step step : steps) {
            if (cancellationMarker.check()) {
                throw new ScenarioCancellationException(step, cancellationMarker);
            }

            final StepAction<Map<String, Object>> stepAction = getStepActionProvider().findAction(step);

            try {
                stepState = stepAction.perform(step, Map.copyOf(stepState));
            } catch (Exception e) {
                throw new StepExecutionException(step, e);
            }
        }

        return stepState;
    }
}
