package io.battery.scenario.step;

import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.battery.model.Step;
import io.battery.script.BatteryScript;

/**
 * Step action executing an inline battery script, whose variables become the state
 * for capturing steps.
 */
@Component
public class ScriptInlineStep implements StepAction<Map<String, Object>> {
    @Autowired
    private BatteryScript batteryScript;

    @Override
    public boolean accepts(Step step) {
        return StringUtils.hasLength(step.getScript())
               && Objects.isNull(step.getPath());
    }

    @Override
    public Map<String, Object> perform(Step step, Map<String, Object> initialState) {
        String script = step.getScript();

        Map<String, Object> result = batteryScript.execute(script, initialState);
        if (step.isCapture()) {
            return result;
        }

        return initialState;
    }
}
