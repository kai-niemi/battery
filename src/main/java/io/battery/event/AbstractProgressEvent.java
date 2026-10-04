package io.battery.event;

import org.springframework.context.ApplicationEvent;

public abstract class AbstractProgressEvent<T> extends ApplicationEvent {
    private final T element;

    private final double progress;

    public AbstractProgressEvent(Object source, T element, double progress) {
        super(source);
        this.element = element;
        this.progress = progress;
    }

    public T getElement() {
        return element;
    }

    public double getProgress() {
        return progress;
    }
}
