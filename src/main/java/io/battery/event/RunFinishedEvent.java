package io.battery.event;

import org.springframework.context.ApplicationEvent;

import io.battery.scenario.run.RunSummary;

/**
 * Published once a scenario run and all its virtual users have completed, which may be
 * after the {@link CompletedEvent} unless the after steps await completion. This ends the
 * scenario launch, as a synchronously delivered lifecycle event.
 */
public class RunFinishedEvent extends ApplicationEvent implements LifecycleEvent {
    private final RunSummary summary;

    public RunFinishedEvent(Object source, RunSummary summary) {
        super(source);
        this.summary = summary;
    }

    public RunSummary getSummary() {
        return summary;
    }
}
