package io.battery.scenario.worker;

/**
 * Lifecycle status of a worker, with the Bootstrap badge class used to display it.
 */
public enum WorkerStatus {
    RUNNING("text-bg-info"),
    COMPLETED("text-bg-success"),
    CANCELLED("text-bg-warning"),
    FAILED("text-bg-danger");

    final String badge;

    WorkerStatus(String badge) {
        this.badge = badge;
    }

    public String getBadge() {
        return badge;
    }
}
