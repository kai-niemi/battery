package io.battery.event;

import org.springframework.context.ApplicationEvent;

import io.battery.model.Scenario;

public class StartedEvent extends ApplicationEvent implements LifecycleEvent {
    private final Scenario scenario;

    private final int runId;

    /**
     * @param runId the number of the run, as in its {@link RunFinishedEvent}
     */
    public StartedEvent(Object source, Scenario scenario, int runId) {
        super(source);
        this.scenario = scenario;
        this.runId = runId;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public int getRunId() {
        return runId;
    }
}
