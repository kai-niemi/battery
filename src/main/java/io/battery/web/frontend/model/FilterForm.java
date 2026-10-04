package io.battery.web.frontend.model;

import io.battery.scenario.worker.WorkerStatus;

public class FilterForm {
    private WorkerStatus status;

    public WorkerStatus getStatus() {
        return status;
    }

    public FilterForm setStatus(WorkerStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "WorkloadFilterForm{" +
               "status=" + status +
               '}';
    }
}
