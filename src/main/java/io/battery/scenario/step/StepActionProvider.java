package io.battery.scenario.step;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.battery.model.ModelException;
import io.battery.model.Step;

/**
 * Resolves the single {@link StepAction} bean accepting a step, cached by step name
 * which is unique within the battery model.
 */
@Component
public class StepActionProvider {
    private final Map<UUID, StepAction<Map<String, Object>>> stepActionCache = new ConcurrentHashMap<>();

    @Autowired
    private ListableBeanFactory beanFactory;

    @SuppressWarnings("unchecked")
    public StepAction<Map<String, Object>> findAction(Step step) {
        Objects.requireNonNull(step, "step is null");
        Objects.requireNonNull(step.getId(), "Step ID is null");
        Objects.requireNonNull(step.getName(), "Step name is null");

        if (!stepActionCache.containsKey(step.getId())) {
            @SuppressWarnings("RedundantExplicitVariableType") Map<String, StepAction> actionMap
                    = BeanFactoryUtils.beansOfTypeIncludingAncestors(beanFactory, StepAction.class);

            long n = actionMap.values().stream().filter(
                    stepAction -> stepAction.accepts(step)).count();
            if (n != 1) {
                throw new ModelException(
                        "Expected single step action for [%s] but found %d - conflicting settings"
                                .formatted(step.getId(), n));
            }

            StepAction<Map<String, Object>> action = actionMap.values().stream()
                    .filter(stepAction -> stepAction.accepts(step))
                    .findFirst()
                    .orElseThrow(() -> new ModelException(
                            "No step action found accepting [%s]".formatted(step.getName())));

            stepActionCache.put(step.getId(), action);
        }

        return stepActionCache.get(step.getId());
    }

    public void evictCache() {
        stepActionCache.clear();
    }
}
