package io.battery.web.api.model;

import java.time.Duration;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import io.battery.metrics.Problem;
import io.battery.scenario.worker.WorkerStatus;

@Relation(value = LinkRelations.WORKER_REL,
        collectionRelation = LinkRelations.WORKERS_REL)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"links", "embedded", "templates"})
public class WorkerModel extends RepresentationModel<WorkerModel> {
    private int id;

    private String scenario;

    private String phase;

    private Duration remainingTime;

    private double opsPerSecond;

    private double p90;

    private double p99;

    private int success;

    private int transientErrors;

    private int nonTransientErrors;

    private WorkerStatus status;

    private Problem lastProblem;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Duration getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(Duration remainingTime) {
        this.remainingTime = remainingTime;
    }

    public double getOpsPerSecond() {
        return opsPerSecond;
    }

    public void setOpsPerSecond(double opsPerSecond) {
        this.opsPerSecond = opsPerSecond;
    }

    public double getP90() {
        return p90;
    }

    public void setP90(double p90) {
        this.p90 = p90;
    }

    public double getP99() {
        return p99;
    }

    public void setP99(double p99) {
        this.p99 = p99;
    }

    public int getSuccess() {
        return success;
    }

    public void setSuccess(int success) {
        this.success = success;
    }

    public int getTransientErrors() {
        return transientErrors;
    }

    public void setTransientErrors(int transientErrors) {
        this.transientErrors = transientErrors;
    }

    public int getNonTransientErrors() {
        return nonTransientErrors;
    }

    public void setNonTransientErrors(int nonTransientErrors) {
        this.nonTransientErrors = nonTransientErrors;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public Problem getLastProblem() {
        return lastProblem;
    }

    public void setLastProblem(Problem lastProblem) {
        this.lastProblem = lastProblem;
    }
}
