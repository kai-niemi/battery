package io.battery.scenario.run;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.scenario.run.RunSummary.Finding;
import io.battery.scenario.run.RunSummary.Severity;
import io.battery.scenario.worker.IterationObserver.Outcome;
import io.battery.scenario.worker.Worker;

import static io.battery.scenario.run.RunTest.T0;
import static io.battery.scenario.run.RunTest.iterations;
import static io.battery.scenario.run.RunTest.phase;
import static io.battery.scenario.run.RunTest.pool;

@Tag("unit-test")
public class RunFindingsTest {
    private static List<CompletableFuture<Worker>> addUsers(Run run, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> run.addWorker(new CompletableFuture<>()))
                .toList();
    }

    private static List<Finding> findings(Run run, int endSecond, RunSummary.Outcome outcome, String reason,
                                          List<Worker> workers) {
        RunSummary summary = run.toSummary(T0.plusSeconds(endSecond), outcome, reason, workers);
        return RunFindings.analyze(summary);
    }

    @Test
    public void givenHealthyRun_expectNoFindings() {
        Run run = new Run(1, "scenario", T0);
        run.phaseStarted(phase("p1", 10), T0);
        addUsers(run, 10);
        iterations(run, 20_000, 5, Outcome.SUCCESS, null);

        Assertions.assertThat(findings(run, 10, RunSummary.Outcome.COMPLETED, null, List.of())).isEmpty();
    }

    @Test
    public void givenSaturatedRun_expectFindingsBySeverity() {
        Run run = new Run(1, "scenario", T0);
        run.sample(T0, pool(0, 0));

        run.phaseStarted(phase("warm up", 10), T0);
        addUsers(run, 10);
        iterations(run, 10_000, 5, Outcome.SUCCESS, null);

        run.phaseStarted(phase("peak", 20), T0.plusSeconds(10));
        addUsers(run, 20);
        // 3x the users but only 1.1x the ops/s, with 4x the p99
        iterations(run, 10_700, 20, Outcome.SUCCESS, null);
        iterations(run, 300, 20, Outcome.TRANSIENT_ERROR, new SQLException("serialization failure", "40001"));
        run.sample(T0.plusSeconds(12), pool(8, 0));
        run.sample(T0.plusSeconds(15), pool(12, 3));
        run.usersDropped(Map.of("peak", 40));
        iterations(run, 1, 20, Outcome.ERROR, new IllegalStateException("boom"));

        Worker failed = new Worker(1, "scenario", "peak", java.time.Duration.ofMinutes(1));
        failed.markFailed();

        List<Finding> findings = findings(run, 20, RunSummary.Outcome.COMPLETED, null, List.of(failed));

        Assertions.assertThat(findings).extracting(Finding::severity)
                .isSortedAccordingTo(java.util.Comparator.naturalOrder());
        Assertions.assertThat(findings.getFirst())
                .extracting(Finding::severity, Finding::message)
                .containsExactly(Severity.ERROR,
                        "1 of 30 users failed with an error that ended them, mostly IllegalStateException: boom.");
        Assertions.assertThat(findings)
                .filteredOn(finding -> "peak".equals(finding.phase()))
                .extracting(Finding::message)
                .anySatisfy(message -> Assertions.assertThat(message)
                        .startsWith("Connection pool saturated: up to 12 users waited for a connection")
                        .contains("3 connection requests timed out"))
                .anySatisfy(message -> Assertions.assertThat(message).startsWith("40 users dropped"))
                .anySatisfy(message -> Assertions.assertThat(message)
                        .startsWith("Throughput levels off: 3.0x the users of warm up (10 to 30)"))
                .anySatisfy(message -> Assertions.assertThat(message)
                        .startsWith("p99 latency rises to 20ms, 4.0x that of warm up"));
        Assertions.assertThat(findings)
                .extracting(Finding::message)
                .anySatisfy(message -> Assertions.assertThat(message)
                        .startsWith("1.4% of iterations (300) failed with transient errors")
                        .endsWith("mostly SQLException [40001]."));
    }

    @Test
    public void givenCancelledShortRun_expectPartialAndSampleSizeFindings() {
        Run run = new Run(1, "scenario", T0);
        run.phaseStarted(phase("p1", 1), T0);
        iterations(run, 50, 5, Outcome.SUCCESS, null);

        Assertions.assertThat(findings(run, 10, RunSummary.Outcome.CANCELLED, "by test", List.of()))
                .extracting(Finding::severity, Finding::message)
                .containsExactly(
                        Assertions.tuple(Severity.WARNING, "Run cancelled (by test), so the results are partial."),
                        Assertions.tuple(Severity.INFO,
                                "Only 50 iterations, too few for a reliable p99. Run longer for tail latencies."));
    }
}
