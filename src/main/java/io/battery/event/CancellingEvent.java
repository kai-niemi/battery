package io.battery.event;

import org.springframework.context.ApplicationEvent;

import io.battery.scenario.CancellationMarker;

public class CancellingEvent extends ApplicationEvent implements LifecycleEvent {
    private final CancellationMarker cancellationMarker;

    public CancellingEvent(Object source, CancellationMarker cancellationMarker) {
        super(source);
        this.cancellationMarker = cancellationMarker;
    }

    public CancellationMarker getCancellationMarker() {
        return cancellationMarker;
    }
}
