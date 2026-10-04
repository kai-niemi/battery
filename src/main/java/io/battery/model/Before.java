package io.battery.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;

/**
 * Steps run once before the phases of a scenario launch, unless the launch request skips them.
 * The state they capture is passed on to every virtual user (VU).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Before {
    private List<@Valid Step> steps = new ArrayList<>();

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
