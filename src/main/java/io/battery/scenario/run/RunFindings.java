package io.battery.scenario.run;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import io.battery.scenario.run.RunSummary.ErrorSummary;
import io.battery.scenario.run.RunSummary.Finding;
import io.battery.scenario.run.RunSummary.Outcome;
import io.battery.scenario.run.RunSummary.PhaseSummary;
import io.battery.scenario.run.RunSummary.Severity;

/**
 * Derives findings from a run summary, such as failed users, a saturated connection pool,
 * dropped users, throughput leveling off or latency degrading between phases. Findings are
 * ordered by severity, with the evidence and what to try next.
 */
public abstract class RunFindings {
    /**
     * @param pendingRatio      share of a phase with users waiting for a connection, above
     *                          which the pool counts as saturated
     * @param transientRate     share of iterations with transient errors worth a warning
     * @param degradation       p99 ratio between phases worth a warning
     * @param kneeUsers         user growth between phases for checking throughput
     * @param kneeEfficiency    share of the user growth that throughput must keep up with
     * @param tailRatio         p99.9 to median ratio counting as a long tail
     * @param minPhaseSamples   iterations a phase needs to compare its latency or throughput
     * @param minP99Samples     iterations for a reliable p99
     * @param minP999Samples    iterations for a reliable p99.9
     */
    public record Thresholds(double pendingRatio, double transientRate, double degradation,
                             double kneeUsers, double kneeEfficiency, double tailRatio,
                             long minPhaseSamples, long minP99Samples, long minP999Samples) {
        public static final Thresholds DEFAULT = new Thresholds(
                0.1, 0.01, 2.0, 1.5, 0.5, 10.0, 100, 1000, 10_000);
    }

    private RunFindings() {
    }

    public static List<Finding> analyze(RunSummary run) {
        return analyze(run, Thresholds.DEFAULT);
    }

    public static List<Finding> analyze(RunSummary run, Thresholds thresholds) {
        List<Finding> findings = new ArrayList<>();

        outcome(run, findings);
        failedUsers(run, findings);
        for (PhaseSummary phase : run.phases()) {
            poolSaturation(phase, thresholds, findings);
            droppedUsers(phase, findings);
        }
        throughputKnee(run, thresholds, findings);
        latencyDegradation(run, thresholds, findings);
        transientErrors(run, thresholds, findings);
        latencyTail(run, thresholds, findings);
        sampleSize(run, thresholds, findings);

        // Stable, so findings of equal severity keep their order
        findings.sort(Comparator.comparing(Finding::severity));
        return List.copyOf(findings);
    }

    private static void outcome(RunSummary run, List<Finding> findings) {
        if (run.outcome() == Outcome.CANCELLED) {
            findings.add(new Finding(Severity.WARNING, null,
                    "Run cancelled (%s), so the results are partial.".formatted(run.outcomeReason())));
        } else if (run.outcome() == Outcome.FAILED) {
            findings.add(new Finding(Severity.ERROR, null,
                    "Run failed: %s. The results may be partial.".formatted(run.outcomeReason())));
        }
        if (run.totals().iterations() == 0 && run.outcome() == Outcome.COMPLETED) {
            findings.add(new Finding(Severity.WARNING, null, "No iterations ran."));
        }
    }

    private static void failedUsers(RunSummary run, List<Finding> findings) {
        if (run.workers().failed() == 0) {
            return;
        }
        String cause = topError(run, false)
                .map(error -> ", mostly %s: %s".formatted(RunFormat.error(error), error.message()))
                .orElse("");
        findings.add(new Finding(Severity.ERROR, null,
                "%d of %d users failed with an error that ended them%s."
                        .formatted(run.workers().failed(), run.workers().started(), cause)));
    }

    private static void poolSaturation(PhaseSummary phase, Thresholds thresholds, List<Finding> findings) {
        if (phase.timeouts() == 0 && phase.pendingRatio() <= thresholds.pendingRatio()) {
            return;
        }
        String timeouts = phase.timeouts() > 0
                ? " and %d connection requests timed out".formatted(phase.timeouts())
                : "";
        findings.add(new Finding(Severity.WARNING, phase.name(),
                ("Connection pool saturated: up to %d users waited for a connection, %s of the time%s. "
                 + "Latency includes the wait. Lower maxConcurrency or raise battery.connectionPool.maxSize.")
                        .formatted(phase.peakPending(), RunFormat.percent(phase.pendingRatio()), timeouts)));
    }

    private static void droppedUsers(PhaseSummary phase, List<Finding> findings) {
        if (phase.usersDropped() > 0) {
            findings.add(new Finding(Severity.WARNING, phase.name(),
                    ("%d users dropped at max concurrency, as arrivals outpaced completing users. "
                     + "The system under test may be saturated, or maxConcurrency is too low.")
                            .formatted(phase.usersDropped())));
        }
    }

    private static List<PhaseSummary> loadPhases(RunSummary run, Thresholds thresholds) {
        return run.phases().stream()
                .filter(phase -> phase.totals().iterations() >= thresholds.minPhaseSamples())
                .toList();
    }

    private static void throughputKnee(RunSummary run, Thresholds thresholds, List<Finding> findings) {
        List<PhaseSummary> phases = loadPhases(run, thresholds);
        for (int i = 1; i < phases.size(); i++) {
            PhaseSummary previous = phases.get(i - 1);
            PhaseSummary phase = phases.get(i);
            if (previous.peakUsers() == 0 || previous.totals().opsPerSecond() == 0) {
                continue;
            }
            double users = (double) phase.peakUsers() / previous.peakUsers();
            double ops = phase.totals().opsPerSecond() / previous.totals().opsPerSecond();
            if (users >= thresholds.kneeUsers() && ops < 1 + (users - 1) * thresholds.kneeEfficiency()) {
                findings.add(new Finding(Severity.INFO, phase.name(),
                        ("Throughput levels off: %s the users of %s (%d to %d) but %s the ops/s. "
                         + "The system under test may be near its capacity.")
                                .formatted(RunFormat.ratio(users), previous.name(), previous.peakUsers(),
                                        phase.peakUsers(), RunFormat.ratio(ops))));
            }
        }
    }

    private static void latencyDegradation(RunSummary run, Thresholds thresholds, List<Finding> findings) {
        List<PhaseSummary> phases = loadPhases(run, thresholds);
        if (phases.size() < 2 || phases.getFirst().latency().p99() <= 0) {
            return;
        }
        PhaseSummary baseline = phases.getFirst();
        // Report the worst phase only, rather than each one past the threshold
        phases.stream()
                .skip(1)
                .max(Comparator.comparingDouble(phase -> phase.latency().p99()))
                .filter(phase -> phase.latency().p99() > baseline.latency().p99() * thresholds.degradation())
                .ifPresent(phase -> findings.add(new Finding(Severity.WARNING, phase.name(),
                        "p99 latency rises to %s, %s that of %s (%s)."
                                .formatted(RunFormat.millis(phase.latency().p99()),
                                        RunFormat.ratio(phase.latency().p99() / baseline.latency().p99()),
                                        baseline.name(), RunFormat.millis(baseline.latency().p99())))));
    }

    private static void transientErrors(RunSummary run, Thresholds thresholds, List<Finding> findings) {
        long transientErrors = run.totals().transientErrors();
        if (transientErrors == 0) {
            return;
        }
        String cause = topError(run, true)
                .map(error -> ", mostly %s".formatted(RunFormat.error(error)))
                .orElse("");
        double rate = run.totals().transientErrorRate();
        findings.add(rate > thresholds.transientRate()
                ? new Finding(Severity.WARNING, null,
                "%s of iterations (%d) failed with transient errors and were retried%s."
                        .formatted(RunFormat.percent(rate), transientErrors, cause))
                : new Finding(Severity.INFO, null,
                "%d transient errors (%s of iterations) were retried%s."
                        .formatted(transientErrors, RunFormat.percent(rate), cause)));
    }

    private static void latencyTail(RunSummary run, Thresholds thresholds, List<Finding> findings) {
        RunSummary.Latency latency = run.latency();
        if (latency.count() >= thresholds.minP999Samples() && latency.p50() > 0
            && latency.p999() > latency.p50() * thresholds.tailRatio()) {
            findings.add(new Finding(Severity.INFO, null,
                    ("Long latency tail: p99.9 %s is %s the median %s. "
                     + "Look for slow statements, lock contention or connection waits.")
                            .formatted(RunFormat.millis(latency.p999()),
                                    RunFormat.ratio(latency.p999() / latency.p50()),
                                    RunFormat.millis(latency.p50()))));
        }
    }

    private static void sampleSize(RunSummary run, Thresholds thresholds, List<Finding> findings) {
        long count = run.latency().count();
        if (count == 0) {
            return;
        }
        if (count < thresholds.minP99Samples()) {
            findings.add(new Finding(Severity.INFO, null,
                    "Only %d iterations, too few for a reliable p99. Run longer for tail latencies."
                            .formatted(count)));
        } else if (count < thresholds.minP999Samples()) {
            findings.add(new Finding(Severity.INFO, null,
                    "%s iterations, too few for a reliable p99.9.".formatted(RunFormat.count(count))));
        }
    }

    private static Optional<ErrorSummary> topError(RunSummary run, boolean transientError) {
        // Errors are ordered by count
        return run.errors().stream()
                .filter(error -> error.transientError() == transientError)
                .findFirst();
    }
}
