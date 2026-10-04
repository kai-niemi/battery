package io.battery.scenario.run;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Summary of a scenario run, from launch until its last virtual user (VU) completed. Phase
 * figures cover the time each phase was active, including VUs created in earlier phases,
 * and a trailing "after phases" entry covers VUs still running once all phases completed.
 * Latencies are in milliseconds and include any time spent waiting for a pooled connection.
 *
 * @param id               run number, starting at 1 for each application start
 * @param loadMillis       time from the start of the first phase to the end of the run
 * @param latencyHistogram the whole-run latency histogram in microseconds, as a base64
 *                         compressed HdrHistogram, so that runs can be merged accurately
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RunSummary(
        int id,
        String scenario,
        Instant startTime,
        Instant endTime,
        long durationMillis,
        long loadMillis,
        Outcome outcome,
        String outcomeReason,
        Totals totals,
        Latency latency,
        Workers workers,
        Pool pool,
        List<PhaseSummary> phases,
        List<ErrorSummary> errors,
        List<Finding> findings,
        String latencyHistogram) {

    public enum Outcome {
        COMPLETED, CANCELLED, FAILED
    }

    /**
     * @param transientErrors iterations failed by a retried transient error
     * @param errors          iterations failed by a non-transient error, which ends the VU
     */
    public record Totals(
            long iterations,
            long succeeded,
            long transientErrors,
            long errors,
            double opsPerSecond,
            double peakOpsPerSecond) {
        public double transientErrorRate() {
            return iterations > 0 ? (double) transientErrors / iterations : 0;
        }

        public double errorRate() {
            return iterations > 0 ? (double) (transientErrors + errors) / iterations : 0;
        }
    }

    public record Latency(
            long count,
            double meanMillis,
            double p50,
            double p90,
            double p99,
            double p999,
            double maxMillis) {
    }

    public record Workers(
            int started,
            int completed,
            int failed,
            int cancelled,
            int peakConcurrent) {
    }

    /**
     * @param peakPending       the most VUs waiting for a connection at once
     * @param timeouts          connection requests timed out during the run
     * @param meanAcquireMillis mean time to acquire a connection during the run
     * @param peakAcquireMillis the highest one second mean acquire time
     */
    public record Pool(
            int maxSize,
            int peakActive,
            int peakPending,
            long timeouts,
            double meanAcquireMillis,
            double peakAcquireMillis) {
    }

    /**
     * @param usersStarted VUs created by this phase
     * @param usersDropped VUs not created because the phase max concurrency was reached
     * @param peakUsers    the most concurrently active VUs, including those from earlier phases
     * @param pendingRatio share of one second samples with VUs waiting for a connection
     */
    public record PhaseSummary(
            String name,
            boolean pause,
            long durationMillis,
            int usersStarted,
            int usersDropped,
            int peakUsers,
            Totals totals,
            Latency latency,
            int peakPending,
            double pendingRatio,
            long timeouts) {
    }

    /**
     * @param transientError whether the error was retried rather than ending the VU
     */
    public record ErrorSummary(
            String type,
            String sqlState,
            String message,
            boolean transientError,
            long count) {
    }

    public enum Severity {
        ERROR, WARNING, INFO
    }

    /**
     * @param phase the phase the finding applies to, if any
     */
    public record Finding(Severity severity, String phase, String message) {
    }

    public RunSummary withFindings(List<Finding> findings) {
        return new RunSummary(id, scenario, startTime, endTime, durationMillis, loadMillis, outcome,
                outcomeReason, totals, latency, workers, pool, phases, errors, findings, latencyHistogram);
    }
}
