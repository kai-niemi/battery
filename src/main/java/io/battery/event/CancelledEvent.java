package io.battery.event;

import java.time.Duration;

import org.springframework.context.ApplicationEvent;

import io.battery.scenario.CancellationMarker;

public class CancelledEvent extends ApplicationEvent implements LifecycleEvent {
    private final Duration duration;

    private final CancellationMarker cancellationMarker;

    public CancelledEvent(Object source, Duration duration, CancellationMarker cancellationMarker) {
        super(source);
        this.duration = duration;
        this.cancellationMarker = cancellationMarker;
    }

    public Duration getDuration() {
        return duration;
    }

    public CancellationMarker getCancellationMarker() {
        return cancellationMarker;
    }
}
