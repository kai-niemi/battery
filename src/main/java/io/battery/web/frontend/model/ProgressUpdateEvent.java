package io.battery.web.frontend.model;

public class ProgressUpdateEvent {
    private final String tag;

    private final double progress;

    public ProgressUpdateEvent(String tag, double progress) {
        this.tag = tag;
        this.progress = progress;
    }

    public double getProgress() {
        return progress;
    }

    public String getTag() {
        return tag;
    }
}
