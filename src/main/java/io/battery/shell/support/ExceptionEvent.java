package io.battery.shell.support;

public class ExceptionEvent {
    private final Throwable throwable;

    public ExceptionEvent(Throwable throwable) {
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
