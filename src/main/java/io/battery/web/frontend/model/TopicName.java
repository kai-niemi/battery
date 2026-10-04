package io.battery.web.frontend.model;

public enum TopicName {
    SCENARIO_PHASE_PROGRESS("/topic/scenario/phase/progress"),
    SCENARIO_PAGE_REFRESH("/topic/scenario/refresh"),

    WORKER_MODEL_UPDATE("/topic/worker/update"),
    WORKER_CHARTS_UPDATE("/topic/worker/charts"),

    SYSTEM_CHARTS_UPDATE("/topic/system/charts");

    final String value;

    TopicName(java.lang.String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
