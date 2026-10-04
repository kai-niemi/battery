package io.battery.model;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import io.battery.util.DurationUtils;

/**
 * A period of a scenario launch that creates virtual users (VUs). A phase either creates a
 * fixed number of {@code users} evenly over its duration, creates VUs at a rate ramping from
 * {@code startRate} to {@code maxRate}, or creates no VUs and only pauses. Phases run in the
 * order they are configured.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@PhaseValidation
public class Phase {
    @NotBlank
    private String name;

    // VU ramping duration
    @NotNull(message = "must be specified in duration format")
    private Duration duration;

    // Total number of VUs created evenly over the duration
    @PositiveOrZero
    private int users;

    // Initial VU creation rate in VUs/sec
    @PositiveOrZero
    private int startRate;

    // Final VU creation rate in VUs/sec, defaults to startRate for a constant rate
    @PositiveOrZero
    private int maxRate;

    // Max number of concurrently active VUs (including VUs from earlier phases) while
    // creating VUs in this phase, beyond which new VUs are dropped. Defaults to 0 for unlimited.
    @PositiveOrZero
    private int maxConcurrency;

    /**
     * @return true if the phase creates no VUs and only pauses for its duration
     */
    public boolean isPausePhase() {
        return users == 0 && startRate == 0 && maxRate == 0;
    }

    /**
     * @return true if the phase creates a total number of VUs evenly over its duration
     */
    public boolean isFixedPhase() {
        return users > 0;
    }

    /**
     * @return true if the phase creates VUs at a rate ramping linearly from
     * startRate to maxRate (or constant at startRate) over its duration
     */
    public boolean isRampingPhase() {
        return users == 0 && (startRate > 0 || maxRate > 0);
    }

    /**
     * @return the final VU creation rate, which is the startRate if maxRate is omitted
     */
    public int getEffectiveMaxRate() {
        return maxRate > 0 ? maxRate : startRate;
    }

    public int getUsers() {
        return users;
    }

    public void setUsers(int users) {
        this.users = users;
    }

    public int getStartRate() {
        return startRate;
    }

    public void setStartRate(int startRate) {
        this.startRate = startRate;
    }

    public int getMaxRate() {
        return maxRate;
    }

    public void setMaxRate(int maxRate) {
        this.maxRate = maxRate;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public Phase setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        return this;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String describe() {
        if (isFixedPhase()) {
            return "%s creating %d users for duration of %s"
                    .formatted(name, users, duration);
        }
        if (isRampingPhase()) {
            return startRate == getEffectiveMaxRate()
                    ? "%s creating %d users/sec for duration of %s"
                    .formatted(name, startRate, duration)
                    : "%s ramping from %d to %d users/sec for duration of %s"
                    .formatted(name, startRate, getEffectiveMaxRate(), duration);
        }
        return "%s pausing for duration of %s".formatted(name, duration);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Phase phase = (Phase) o;
        return name.equals(phase.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Phase{" +
               "name='" + name + '\'' +
               ", duration='" + duration + '\'' +
               ", users=" + users +
               ", startRate=" + startRate +
               ", maxRate=" + maxRate +
               ", maxConcurrency=" + maxConcurrency +
               '}';
    }
}
