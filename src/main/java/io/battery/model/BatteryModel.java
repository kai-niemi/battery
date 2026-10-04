package io.battery.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import io.battery.util.RandomData;

/**
 * The workload model bound from the {@code battery} configuration properties: the
 * {@link Phase phases} that create virtual users (VUs), the {@link Scenario scenarios} they run,
 * the {@link Before before} and {@link After after} steps run once per launch, the
 * {@link Network network} of remote agents and the {@link ConnectionPool connection pool}
 * sizing.
 * <p>
 * On startup, step paths are resolved against {@code baseDir} and step names are checked to be
 * unique across the whole model.
 */
@Validated
@ConfigurationProperties(prefix = "battery", ignoreUnknownFields = false)
public class BatteryModel {
    private Path baseDir;

    @Valid
    private Before before = new Before();

    @Valid
    private After after = new After();

    @NotEmpty(message = "Phases cannot be empty - this can be caused by a missing 'phases' section or a misconfigured application profile name.")
    private List<@Valid Phase> phases = new ArrayList<>();

    @NotEmpty(message = "Scenarios cannot be empty - this can be caused by a missing 'scenarios' section or a misconfigured application profile name.")
    private List<@Valid Scenario> scenarios = new ArrayList<>();

    @Valid
    private Network network = new Network();

    @Valid
    private ConnectionPool connectionPool = new ConnectionPool();

    @PostConstruct
    public void init() {
        before.getSteps().forEach(step -> {
            validateStep(step);
            step.setTag("before-steps"); // div element id
        });
        after.getSteps().forEach(step -> {
            validateStep(step);
            step.setTag("after-steps"); // div element id
        });

        for (Scenario scenario : scenarios) {
            scenario.getSteps().forEach(this::validateStep);
        }

        // Default to the first scenario, unless one is explicitly marked primary
        if (!scenarios.isEmpty() && scenarios.stream().noneMatch(Scenario::isPrimary)) {
            scenarios.getFirst().setPrimary(true);
        }
    }

    private void validateStep(Step step) {
        step.resolvePath(baseDir);
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    public List<Phase> getPhases(Predicate<Phase> predicate) {
        return phases.stream().filter(predicate).toList();
    }

    public List<Phase> getPhases() {
        return phases;
    }

    public void setPhases(List<Phase> phases) {
        this.phases = phases;
    }

    public After getAfter() {
        return after;
    }

    public void setAfter(After after) {
        this.after = after;
    }

    public Before getBefore() {
        return before;
    }

    public void setBefore(Before before) {
        this.before = before;
    }

    public Optional<Scenario> getPrimaryScenario() {
        return scenarios.stream()
                .filter(Scenario::isPrimary)
                .findFirst();
    }

    public List<Scenario> getScenarios() {
        return scenarios;
    }

    public void setScenarios(List<Scenario> scenarios) {
        this.scenarios = scenarios;
    }

    public Network getNetwork() {
        return network;
    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    public void setConnectionPool(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public Optional<Scenario> findRandomScenario() {
        if (scenarios.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(RandomData.selectRandomWeighted(scenarios, scenarios.stream()
                .map(Scenario::getWeight)
                .collect(Collectors.toList())));
    }

    public Optional<Scenario> findNamedScenario(String nameOrAlias) {
        Objects.requireNonNull(nameOrAlias);
        return scenarios.stream()
                .filter(scenario -> nameOrAlias.equalsIgnoreCase(scenario.getName())
                                    || nameOrAlias.equalsIgnoreCase(scenario.getAlias()))
                .findFirst();
    }
}
