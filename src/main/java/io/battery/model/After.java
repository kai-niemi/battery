package io.battery.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;

/**
 * Steps run once after the phases of a scenario launch have completed successfully, unless the
 * launch request skips them. By default they run as soon as the last phase has created its
 * virtual users (VUs); set {@code awaitCompletion} to first wait for all VUs to finish.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class After {
    private List<@Valid Step> steps = new ArrayList<>();

    private boolean awaitCompletion;

    public boolean isAwaitCompletion() {
        return awaitCompletion;
    }

    public void setAwaitCompletion(boolean awaitCompletion) {
        this.awaitCompletion = awaitCompletion;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    @JsonIgnore
    public int getStepCount() {
        return steps.size();
    }

    @JsonIgnore
    public List<String> getStepNames() {
        return steps.stream().map(Step::getName).toList();
    }
}
