package io.battery.scenario.step;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.battery.model.ScriptFormat;
import io.battery.model.Step;
import io.battery.script.BatteryScript;

/**
 * Step action executing a battery script file given by the step path, whose variables
 * become the state for capturing steps.
 */
@Component
public class ScriptFileStep implements StepAction<Map<String, Object>> {
    @Autowired
    private BatteryScript batteryScript;

    @Override
    public boolean accepts(Step step) {
        return !StringUtils.hasLength(step.getScript())
               && Objects.nonNull(step.getPath())
               && ScriptFormat.BATTERY.accepts(step.getPath());
    }

    @Override
    public Map<String, Object> perform(Step step, Map<String, Object> initialState) {
        Path scriptPath = step.getPath();

        Map<String, Object> result = batteryScript.execute(scriptPath, initialState);
        if (step.isCapture()) {
            return result;
        }

        // Not capturing means this step contributes nothing, not that it wipes out
        // the state captured by preceding steps.
        return initialState;
    }
}
