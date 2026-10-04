package io.battery.scenario.run;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.HdrHistogram.Histogram;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.battery.config.JacksonConfig;
import io.battery.model.Phase;
import io.battery.scenario.run.RunSummary.PhaseSummary;
import io.battery.scenario.worker.IterationObserver.Outcome;
import io.battery.scenario.worker.Worker;

@Tag("unit-test")
public class RunTest {
    static final Instant T0 = Instant.parse("2026-10-03T12:00:00Z");

    static Phase phase(String name, int users) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setDuration(Duration.ofSeconds(10));
        phase.setUsers(users);
        return phase;
    }

    static Worker worker(int id, String phase) {
        return new Worker(id, "scenario", phase, Duration.ofMinutes(1));
    }

    static void iterations(Run run, int count, long millis, Outcome outcome, Throwable cause) {
        for (int i = 0; i < count; i++) {
            run.onIteration(Duration.ofMillis(millis), outcome, cause);
        }
    }

    static Run.PoolSample pool(int pending, long timeouts) {
        return new Run.PoolSample(32, 32, pending, timeouts, 100, 50.0);
    }

    @Test
    public void givenPhases_expectIterationsAttributedToActivePhase() {
        Run run = new Run(1, "scenario", T0);
        run.sample(T0, pool(0, 0));

        run.phaseStarted(phase("warm up", 2), T0);
        CompletableFuture<Worker> first = run.addWorker(new CompletableFuture<>());
        CompletableFuture<Worker> second = run.addWorker(new CompletableFuture<>());
        iterations(run, 100, 10, Outcome.SUCCESS, null);
        run.sample(T0.plusSeconds(5), pool(3, 0));

        run.phaseStarted(phase("peak", 1), T0.plusSeconds(10));
        CompletableFuture<Worker> third = run.addWorker(new CompletableFuture<>());
        iterations(run, 50, 100, Outcome.SUCCESS, null);
        iterations(run, 5, 100, Outcome.TRANSIENT_ERROR, new SQLException("retry", "40001"));
        run.sample(T0.plusSeconds(15), pool(0, 2));
        run.usersDropped(Map.of("peak", 7));

        run.phasesCompleted(T0.plusSeconds(20));
        iterations(run, 10, 10, Outcome.SUCCESS, null);

        Worker completed1 = worker(1, "warm up");
        completed1.markCompleted();
        Worker completed2 = worker(2, "warm up");
        completed2.markCompleted();
        Worker failed = worker(3, "peak");
        failed.markFailed();
        first.complete(completed1);
        second.complete(completed2);
        third.complete(failed);

        RunSummary summary = run.toSummary(T0.plusSeconds(30), RunSummary.Outcome.COMPLETED, null,
                List.of(first.join(), second.join(), third.join()));

        Assertions.assertThat(summary.phases())
                .extracting(PhaseSummary::name)
                .containsExactly("warm up", "peak", Run.AFTER_PHASES);

        PhaseSummary warmUp = summary.phases().get(0);
        Assertions.assertThat(warmUp.durationMillis()).isEqualTo(10_000);
        Assertions.assertThat(warmUp.usersStarted()).isEqualTo(2);
        Assertions.assertThat(warmUp.totals().iterations()).isEqualTo(100);
        Assertions.assertThat(warmUp.totals().opsPerSecond()).isEqualTo(10.0);
        Assertions.assertThat(warmUp.latency().p99()).isCloseTo(10.0, Assertions.within(0.1));
        Assertions.assertThat(warmUp.peakPending()).isEqualTo(3);

        PhaseSummary peak = summary.phases().get(1);
        // Users from earlier phases count towards the phase concurrency
        Assertions.assertThat(peak.usersStarted()).isEqualTo(1);
        Assertions.assertThat(peak.peakUsers()).isEqualTo(3);
        Assertions.assertThat(peak.usersDropped()).isEqualTo(7);
        Assertions.assertThat(peak.totals().transientErrors()).isEqualTo(5);
        Assertions.assertThat(peak.latency().p50()).isCloseTo(100.0, Assertions.within(0.1));
        Assertions.assertThat(peak.timeouts()).isEqualTo(2);

        Assertions.assertThat(summary.phases().get(2).totals().iterations()).isEqualTo(10);

        Assertions.assertThat(summary.totals().iterations()).isEqualTo(165);
        Assertions.assertThat(summary.durationMillis()).isEqualTo(30_000);
        Assertions.assertThat(summary.workers())
                .isEqualTo(new RunSummary.Workers(3, 2, 1, 0, 3));
        Assertions.assertThat(summary.pool().timeouts()).isEqualTo(2);
        Assertions.assertThat(summary.pool().peakPending()).isEqualTo(3);
        Assertions.assertThat(summary.errors())
                .singleElement()
                .isEqualTo(new RunSummary.ErrorSummary("SQLException", "40001", "retry", true, 5));
    }

    @Test
    public void givenRun_expectDecodableLatencyHistogram() throws Exception {
        Run run = new Run(1, "scenario", T0);
        run.phaseStarted(phase("p", 1), T0);
        iterations(run, 1000, 5, Outcome.SUCCESS, null);

        RunSummary summary = run.toSummary(T0.plusSeconds(10), RunSummary.Outcome.COMPLETED, null, List.of());

        Histogram histogram = Histogram.decodeFromCompressedByteBuffer(
                ByteBuffer.wrap(Base64.getDecoder().decode(summary.latencyHistogram())), 0);
        Assertions.assertThat(histogram.getTotalCount()).isEqualTo(1000);
        Assertions.assertThat(histogram.getValueAtPercentile(50.0) / 1000.0).isCloseTo(5.0, Assertions.within(0.01));
        // No pool samples without a pool
        Assertions.assertThat(summary.pool()).isNull();
    }

    @Test
    public void givenSummary_expectJsonRoundTrip() throws Exception {
        Run run = new Run(1, "scenario", T0);
        run.phaseStarted(phase("p", 1), T0);
        iterations(run, 10, 5, Outcome.SUCCESS, null);
        RunSummary summary = run.toSummary(T0.plusSeconds(10), RunSummary.Outcome.CANCELLED, "by test", List.of());
        summary = summary.withFindings(RunFindings.analyze(summary));

        var jsonMapper = new JacksonConfig().primaryObjectMapper();
        String json = jsonMapper.writeValueAsString(summary);

        Assertions.assertThat(json).contains("\"startTime\" : \"2026-10-03T12:00:00Z\"", "\"outcome\" : \"CANCELLED\"");
        Assertions.assertThat(jsonMapper.readValue(json, RunSummary.class)).isEqualTo(summary);
    }
}
