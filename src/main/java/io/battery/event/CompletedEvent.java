package io.battery.event;

import java.time.Duration;

import org.springframework.context.ApplicationEvent;

public class CompletedEvent extends ApplicationEvent implements LifecycleEvent {
    private final Duration duration;

    private final Throwable throwable;

    public CompletedEvent(Object source, Duration duration, Throwable throwable) {
        super(source);
        this.duration = duration;
        this.throwable = throwable;
    }

    public Duration getDuration() {
        return duration;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
