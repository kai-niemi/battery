package io.battery.scenario.run;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.json.JsonMapper;

import io.battery.event.RunFinishedEvent;
import io.battery.model.Scenario;
import io.battery.scenario.run.RunSummary.Outcome;
import io.battery.scenario.run.RunSummary.Severity;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Records scenario runs: numbers each run, samples the active runs once a second, and
 * finishes a run once all its virtual users have completed. A finished run is summarized
 * with {@link RunFindings findings}, kept among the most recent runs, logged, written as JSON
 * to a {@code runs} directory next to the log file, and published as a {@link RunFinishedEvent}.
 */
@Component
public class RunRecorder {
    private static final int RECENT_RUNS = 20;

    private static final DateTimeFormatter FILE_TIME
            = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicInteger runNumber = new AtomicInteger();

    private final Set<Run> activeRuns = ConcurrentHashMap.newKeySet();

    // Newest first
    private final Deque<RunSummary> recentRuns = new ArrayDeque<>();

    private final ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("run-sampler").factory());

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private ObjectProvider<HikariDataSource> dataSource;

    @Autowired
    private ObjectProvider<MeterRegistry> meterRegistry;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${logging.file.name:.log/battery.log}")
    private String logFile;

    public RunRecorder() {
        sampler.scheduleAtFixedRate(this::sampleActiveRuns, 1, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        sampler.shutdownNow();
    }

    /**
     * Starts recording a run of the scenario.
     */
    public Run start(Scenario scenario) {
        Run run = new Run(runNumber.incrementAndGet(), scenario.getName());
        // Baseline for the cumulative pool counters
        run.sample(Instant.now(), samplePool());
        activeRuns.add(run);
        return run;
    }

    /**
     * The outcome of a run, with the reason for a cancellation or failure.
     */
    public record Ending(Outcome outcome, String reason) {
    }

    /**
     * Finishes the run once all its virtual users have completed, which requires all of
     * them to have been added to the run. The run always finishes with a published
     * {@link RunFinishedEvent}, with a summary of only the outcome if summarizing fails.
     *
     * @param ending the outcome, decided once the users have completed, since a run may
     *               be cancelled until then
     * @return a future completed with the run summary
     */
    public CompletableFuture<RunSummary> finish(Run run, Supplier<Ending> ending) {
        return run.workersCompleted()
                .thenApply(workers -> {
                    activeRuns.remove(run);
                    Instant now = Instant.now();
                    Ending end = ending.get();
                    RunSummary summary;
                    try {
                        summary = run.toSummary(now, end.outcome(), end.reason(), workers);
                        summary = summary.withFindings(RunFindings.analyze(summary));
                    } catch (RuntimeException e) {
                        logger.error("Failed to summarize run #%d".formatted(run.getId()), e);
                        summary = run.outcomeSummary(now, end.outcome(),
                                "%s (unable to summarize the run: %s)".formatted(end.reason(), e));
                    }
                    finished(summary);
                    return summary;
                });
    }

    private void finished(RunSummary summary) {
        synchronized (recentRuns) {
            recentRuns.addFirst(summary);
            while (recentRuns.size() > RECENT_RUNS) {
                recentRuns.removeLast();
            }
        }

        Optional<Path> file = write(summary);

        logger.info("Run #%d [%s] %s in %s: %s iterations, %s ops/s, p99 %s, %s errors%s".formatted(
                summary.id(), summary.scenario(), summary.outcome(),
                RunFormat.millis(summary.durationMillis()),
                RunFormat.count(summary.totals().iterations()),
                RunFormat.rate(summary.totals().opsPerSecond()),
                RunFormat.millis(summary.latency().p99()),
                RunFormat.percent(summary.totals().errorRate()),
                file.map(path -> " (summary in %s)".formatted(path)).orElse("")));

        summary.findings().forEach(finding -> {
            String message = finding.phase() != null
                    ? "Run #%d [%s] %s".formatted(summary.id(), finding.phase(), finding.message())
                    : "Run #%d %s".formatted(summary.id(), finding.message());
            if (finding.severity() == Severity.INFO) {
                logger.info(message);
            } else {
                logger.warn(message);
            }
        });

        applicationEventPublisher.publishEvent(new RunFinishedEvent(this, summary));
    }

    private Optional<Path> write(RunSummary summary) {
        Path parent = Path.of(logFile).toAbsolutePath().getParent();

        Path file = parent.resolve("runs").resolve("run-%s-%d.json"
                .formatted(FILE_TIME.format(summary.startTime()), summary.id()));

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, jsonMapper.writeValueAsString(summary));
            return Optional.of(file);
        } catch (IOException | RuntimeException e) {
            logger.warn("Unable to write run summary to %s: %s".formatted(file, e.getMessage()));
            return Optional.empty();
        }
    }

    /**
     * @return the most recent run summaries, newest first
     */
    public List<RunSummary> getRecentRuns() {
        synchronized (recentRuns) {
            return List.copyOf(recentRuns);
        }
    }

    public Optional<RunSummary> getRun(int id) {
        return getRecentRuns().stream().filter(summary -> summary.id() == id).findFirst();
    }

    public Optional<RunSummary> getLastRun() {
        return getRecentRuns().stream().findFirst();
    }

    private void sampleActiveRuns() {
        if (activeRuns.isEmpty()) {
            return;
        }
        try {
            Instant now = Instant.now();
            Run.PoolSample pool = samplePool();
            activeRuns.forEach(run -> run.sample(now, pool));
        } catch (RuntimeException e) {
            // Keep sampling, since a failed task would cancel the schedule
            logger.warn("Run sampling failed", e);
        }
    }

    private Run.PoolSample samplePool() {
        HikariDataSource hikariDataSource = dataSource.getIfAvailable();
        HikariPoolMXBean pool = hikariDataSource != null ? hikariDataSource.getHikariPoolMXBean() : null;
        if (pool == null) {
            // No pool, or not started yet
            return null;
        }

        long timeoutCount = -1;
        long acquireCount = -1;
        double acquireTotalMillis = 0;
        MeterRegistry registry = meterRegistry.getIfAvailable();
        if (registry != null) {
            String poolName = hikariDataSource.getPoolName();
            Counter timeouts = registry.find("hikaricp.connections.timeout").tag("pool", poolName).counter();
            if (timeouts != null) {
                timeoutCount = (long) timeouts.count();
            }
            Timer acquire = registry.find("hikaricp.connections.acquire").tag("pool", poolName).timer();
            if (acquire != null) {
                acquireCount = acquire.count();
                acquireTotalMillis = acquire.totalTime(TimeUnit.MILLISECONDS);
            }
        }

        return new Run.PoolSample(hikariDataSource.getMaximumPoolSize(), pool.getActiveConnections(),
                pool.getThreadsAwaitingConnection(), timeoutCount, acquireCount, acquireTotalMillis);
    }
}
