package io.battery.scenario;

import java.util.HashSet;
import java.util.Set;

/**
 * A value object representing a request to start a load test scenario, with options
 * to skip the before steps, after steps or phases. A random scenario is launched if no
 * name is given.
 */
public class ScenarioRequest {
    public static ScenarioRequest newInstance() {
        return new ScenarioRequest();
    }

    private String name;

    private boolean skipBeforeSteps;

    private boolean skipAfterSteps;

    private boolean skipAllPhases;

    private Set<String> skipPhases = new HashSet<>();

    public ScenarioRequest setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }

    public boolean isSkipBeforeSteps() {
        return skipBeforeSteps;
    }

    public ScenarioRequest setSkipBeforeSteps(boolean skipBeforeSteps) {
        this.skipBeforeSteps = skipBeforeSteps;
        return this;
    }

    public boolean isSkipAfterSteps() {
        return skipAfterSteps;
    }

    public ScenarioRequest setSkipAfterSteps(boolean skipAfterSteps) {
        this.skipAfterSteps = skipAfterSteps;
        return this;
    }

    public Set<String> getSkipPhases() {
        return skipPhases;
    }

    public ScenarioRequest setSkipPhases(Set<String> skipPhases) {
        this.skipPhases = skipPhases;
        return this;
    }

    public boolean isSkipAllPhases() {
        return skipAllPhases;
    }

    public ScenarioRequest setSkipAllPhases(boolean skipAllPhases) {
        this.skipAllPhases = skipAllPhases;
        return this;
    }
}
