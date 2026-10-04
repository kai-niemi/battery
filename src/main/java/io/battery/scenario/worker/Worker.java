package io.battery.scenario.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.battery.metrics.Metrics;
import io.battery.metrics.Problem;
import io.battery.util.DurationUtils;

/**
 * A worker representing a single virtual user, holding its lifecycle status, call metrics
 * and problems for display while running and after completion.
 * <p>
 * Updated by the worker thread and read concurrently by monitoring threads, so the
 * status, end time and problems are safe for concurrent access.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Worker {
    private final Integer id;

    private final String scenario;

    private final String phase;

    private final Metrics metrics;

    private final List<Problem> problems = new CopyOnWriteArrayList<>();

    private final Instant startTime;

    // Planned finish time based on the scenario duration
    private final Instant finishTime;

    // Actual end time, set when the worker leaves the running status
    private volatile Instant endTime;

    private volatile WorkerStatus status;

    public Worker(Integer id, String scenario, String phase, Duration duration) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(scenario);
        Objects.requireNonNull(phase);
        Objects.requireNonNull(duration);

        this.id = id;
        this.scenario = scenario;
        this.phase = phase;

        this.metrics = Metrics.empty();
        this.startTime = Instant.now();
        this.finishTime = startTime.plus(duration);
        this.status = WorkerStatus.RUNNING;
    }

    public Integer getId() {
        return id;
    }

    public String getScenario() {
        return scenario;
    }

    public String getPhase() {
        return phase;
    }

    public void addProblem(Problem problem) {
        problems.add(problem);
    }

    public List<Problem> getProblems() {
        return List.copyOf(problems);
    }

    /**
     * @return the most recent problems, newest first
     */
    public List<Problem> getProblems(int limit) {
        return getProblems().reversed().stream().limit(limit).toList();
    }

    public void clearProblem() {
        problems.clear();
    }

    public Problem getLastProblem() {
        // Snapshot to avoid racing with concurrent additions or clearing
        Problem[] snapshot = problems.toArray(new Problem[0]);
        return snapshot.length == 0 ? null : snapshot[snapshot.length - 1];
    }

    public Metrics getMetrics() {
        return isRunning() ? metrics : Metrics.copy(metrics);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getFinishTime() {
        return finishTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Duration getRemainingDuration() {
        return isRunning()
                ? Duration.between(Instant.now(), this.finishTime)
                : Duration.ofSeconds(0);
    }

    public String getRemainingTime() {
        return getRemainingDuration().isPositive()
                ? DurationUtils.durationToDisplayString(getRemainingDuration())
                : "-";
    }

    public Duration getCompletionDuration() {
        Instant end = endTime;
        return end == null
                ? Duration.ofSeconds(0)
                : Duration.between(startTime, end);
    }

    public String getCompletionTime() {
        return getCompletionDuration().isPositive()
                ? DurationUtils.durationToDisplayString(getCompletionDuration())
                : "-";
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public String getStatusBadge() {
        return status.getBadge();
    }

    public boolean isRunning() {
        return WorkerStatus.RUNNING.equals(status);
    }

    public boolean isCompleted() {
        return WorkerStatus.COMPLETED.equals(status);
    }

    public boolean isCancelled() {
        return WorkerStatus.CANCELLED.equals(status);
    }

    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    public void markCancelled() {
        markEnded(WorkerStatus.CANCELLED);
    }

    public void markFailed() {
        markEnded(WorkerStatus.FAILED);
    }

    public void markCompleted() {
        markEnded(WorkerStatus.COMPLETED);
    }

    private void markEnded(WorkerStatus endStatus) {
        // Record the end time before the status, so a non-running status implies an end time
        this.endTime = Instant.now();
        this.status = endStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Worker worker = (Worker) o;
        return Objects.equals(id, worker.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
