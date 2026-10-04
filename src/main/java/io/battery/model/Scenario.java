package io.battery.model;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.hateoas.server.core.Relation;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.battery.util.DurationUtils;
import io.battery.web.api.model.LinkRelations;

/**
 * A sequence of {@link Step steps} that each virtual user (VU) runs repeatedly for the
 * scenario's duration. A launch runs a scenario by name or alias, or one picked at random by
 * {@code weight} when no name is given. The primary scenario is preselected in the web UI and
 * API forms, and is the first scenario unless another one is marked primary.
 * <p>
 * Transient SQL errors are by default counted as failures and retried after a backoff delay,
 * rather than ending the VU.
 */
@Relation(value = LinkRelations.SCENARIO_REL,
        collectionRelation = LinkRelations.SCENARIOS_REL)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scenario {
    @NotBlank
    private String name;

    private String alias;

    @NotNull
    private Duration duration;

    @NotEmpty(message = "Steps cannot be empty.")
    private List<@Valid Step> steps = new ArrayList<>();

    @Min(1)
    private double weight = 1.0;

    private boolean transactional;

    private boolean continueOnTransientErrors = true;

    private boolean backoffOnTransientErrors = true;

    private boolean primary;

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isContinueOnTransientErrors() {
        return continueOnTransientErrors;
    }

    public void setContinueOnTransientErrors(boolean continueOnTransientErrors) {
        this.continueOnTransientErrors = continueOnTransientErrors;
    }

    public boolean isBackoffOnTransientErrors() {
        return backoffOnTransientErrors;
    }

    public void setBackoffOnTransientErrors(boolean backoffOnTransientErrors) {
        this.backoffOnTransientErrors = backoffOnTransientErrors;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return StringUtils.hasLength(alias) ? alias : name;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getDurationFormatted() {
        return DurationUtils.durationToDisplayString(getDuration());
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public boolean isTransactional() {
        return transactional;
    }

    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }

    /**
     * @param iteration the current 1-based iteration of the calling virtual user
     * @return the steps applicable to the given iteration, excluding disabled steps
     * and steps that have exceeded their max iteration count
     */
    public List<Step> getSteps(int iteration) {
        return steps.stream()
                .filter(step -> !step.isSkip())
                .filter(step -> step.getMaxIterations() <= 0
                                || iteration <= step.getMaxIterations())
                .toList();
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @return a single-line summary of the scenario, such as
     * {@code "Scenario 'orders' (alias 'o') running 3 of 4 step(s) for 5m, transactional, weight 2.0, primary"}
     */
    public String describe() {
        StringBuilder sb = new StringBuilder("Scenario '%s'".formatted(name));

        if (StringUtils.hasLength(alias) && !alias.equals(name)) {
            sb.append(" (alias '%s')".formatted(alias));
        }

        long enabledSteps = steps.stream().filter(step -> !step.isSkip()).count();
        if (enabledSteps == steps.size()) {
            sb.append(" running %d step(s)".formatted(steps.size()));
        } else {
            sb.append(" running %d of %d step(s)".formatted(enabledSteps, steps.size()));
        }

        if (duration != null) {
            sb.append(" for ").append(getDurationFormatted());
        }

        sb.append(transactional ? ", transactional" : ", non-transactional");

        if (weight != 1.0) {
            sb.append(", weight %.1f".formatted(weight));
        }
        if (primary) {
            sb.append(", primary");
        }
        if (!continueOnTransientErrors) {
            sb.append(", stops on transient errors");
        } else if (!backoffOnTransientErrors) {
            sb.append(", retries transient errors without backoff");
        }

        return sb.toString();
    }
}
