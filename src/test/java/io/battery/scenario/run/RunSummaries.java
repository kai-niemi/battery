package io.battery.scenario.run;

import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import io.battery.scenario.worker.IterationObserver.Outcome;
import io.battery.scenario.worker.Worker;

import static io.battery.scenario.run.RunTest.T0;
import static io.battery.scenario.run.RunTest.iterations;
import static io.battery.scenario.run.RunTest.phase;
import static io.battery.scenario.run.RunTest.pool;

/**
 * Run summaries for tests outside this package, recorded through a {@link Run}.
 */
public abstract class RunSummaries {
    private RunSummaries() {
    }

    /**
     * @return a summary of a saturated run, with findings of each severity, phases with
     * dropped users and pool waits, a failed user and transient errors
     */
    public static RunSummary saturatedRun(int id) {
        Run run = new Run(id, "Insert tokens", T0);
        run.sample(T0, pool(0, 0));

        run.phaseStarted(phase("Warm up phase", 10), T0);
        IntStream.range(0, 10).forEach(i -> run.addWorker(new CompletableFuture<>()));
        iterations(run, 10_000, 5, Outcome.SUCCESS, null);

        run.phaseStarted(phase("Peak phase", 20), T0.plusSeconds(10));
        IntStream.range(0, 20).forEach(i -> run.addWorker(new CompletableFuture<>()));
        iterations(run, 10_700, 20, Outcome.SUCCESS, null);
        iterations(run, 300, 20, Outcome.TRANSIENT_ERROR, new SQLException("serialization failure", "40001"));
        run.sample(T0.plusSeconds(15), pool(12, 3));
        run.usersDropped(Map.of("Peak phase", 40));
        iterations(run, 1, 20, Outcome.ERROR, new IllegalStateException("boom"));
        run.phasesCompleted(T0.plusSeconds(20));
        // Users still running after the last phase
        iterations(run, 500, 5, Outcome.SUCCESS, null);

        Worker failed = new Worker(1, "Insert tokens", "Peak phase", Duration.ofMinutes(1));
        failed.markFailed();

        RunSummary summary = run.toSummary(T0.plusSeconds(25), RunSummary.Outcome.COMPLETED, null, List.of(failed));
        return summary.withFindings(RunFindings.analyze(summary));
    }

    /**
     * @return a summary of only the outcome of a run
     */
    public static RunSummary outcome(int id, RunSummary.Outcome outcome, String reason) {
        return new Run(id, "test", T0).outcomeSummary(T0.plusSeconds(1), outcome, reason);
    }

    /**
     * @return a summary of a run cancelled before any iterations, without pool samples
     */
    public static RunSummary emptyCancelledRun(int id) {
        Run run = new Run(id, "Insert tokens", T0);
        RunSummary summary = run.toSummary(T0.plusSeconds(1), RunSummary.Outcome.CANCELLED, "by operator",
                List.of());
        return summary.withFindings(RunFindings.analyze(summary));
    }
}
