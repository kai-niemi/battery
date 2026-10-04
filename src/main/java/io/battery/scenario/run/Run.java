package io.battery.scenario.run;

import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;

import io.battery.model.Phase;
import io.battery.scenario.run.RunSummary.ErrorSummary;
import io.battery.scenario.run.RunSummary.Latency;
import io.battery.scenario.run.RunSummary.PhaseSummary;
import io.battery.scenario.run.RunSummary.Totals;
import io.battery.scenario.worker.IterationObserver;
import io.battery.scenario.worker.Worker;
import io.battery.scenario.worker.WorkerStatus;

/**
 * The recording of a scenario run in progress, summarized by {@link #toSummary} once all its
 * virtual users (VUs) have completed.
 * <p>
 * Iterations, VU starts and samples are recorded for the whole run and for the phase active
 * at the time, so phase figures include VUs created in earlier phases. Iterations after the
 * last phase completed are recorded in a trailing "after phases" segment. VU threads record
 * iterations concurrently, while the {@link RunRecorder} samples throughput, concurrency and
 * the connection pool once a second from a single thread.
 */
public class Run implements IterationObserver {
    static final String AFTER_PHASES = "after phases";

    private static final int MAX_ERRORS = 20;

    private static final int MAX_MESSAGE_LENGTH = 200;

    /**
     * A sample of the connection pool, with cumulative timeout and acquire counts or -1 if
     * the pool metrics are unavailable.
     */
    record PoolSample(int maxSize, int active, int pending,
                      long timeoutCount, long acquireCount, double acquireTotalMillis) {
    }

    private record ErrorKey(String type, String sqlState, String message, boolean transientError) {
        static ErrorKey of(Throwable cause, boolean transientError) {
            String sqlState = cause instanceof SQLException sqlException ? sqlException.getSQLState() : null;
            String message = cause.getMessage();
            if (message != null && message.length() > MAX_MESSAGE_LENGTH) {
                message = message.substring(0, MAX_MESSAGE_LENGTH) + "...";
            }
            return new ErrorKey(cause.getClass().getSimpleName(), sqlState, message, transientError);
        }
    }

    /**
     * Recording of a phase or of the whole run. Counters are updated by VU threads and the
     * sampled fields by the single sampling thread, so the latter are only volatile.
     */
    static final class Segment {
        final String name;

        final boolean pause;

        final Instant startTime;

        volatile Instant endTime;

        final Histogram histogram = new ConcurrentHistogram(3);

        final LongAdder succeeded = new LongAdder();

        final LongAdder transientErrors = new LongAdder();

        final LongAdder errors = new LongAdder();

        final AtomicInteger usersStarted = new AtomicInteger();

        final AtomicInteger peakUsers = new AtomicInteger();

        private volatile Instant lastSampleTime;

        private volatile long lastIterations;

        volatile double peakOpsPerSecond;

        volatile int peakPending;

        volatile int poolSamples;

        volatile int pendingSamples;

        volatile long timeouts;

        Segment(String name, boolean pause, Instant startTime) {
            this.name = name;
            this.pause = pause;
            this.startTime = startTime;
            this.lastSampleTime = startTime;
        }

        long iterations() {
            return succeeded.sum() + transientErrors.sum() + errors.sum();
        }

        void record(Duration duration, IterationObserver.Outcome outcome) {
            histogram.recordValue(Math.max(0, duration.toNanos() / 1000));
            switch (outcome) {
                case SUCCESS -> succeeded.increment();
                case TRANSIENT_ERROR -> transientErrors.increment();
                case ERROR -> errors.increment();
            }
        }

        void userStarted(int activeUsers) {
            usersStarted.incrementAndGet();
            peakUsers.accumulateAndGet(activeUsers, Math::max);
        }

        // Called by the sampling thread only
        void sample(Instant now, int activeUsers, PoolSample pool, long timeoutDelta) {
            long iterations = iterations();
            double seconds = Duration.between(lastSampleTime, now).toNanos() / 1e9;
            if (seconds > 0) {
                peakOpsPerSecond = Math.max(peakOpsPerSecond, (iterations - lastIterations) / seconds);
            }
            lastSampleTime = now;
            lastIterations = iterations;

            peakUsers.accumulateAndGet(activeUsers, Math::max);

            if (pool != null) {
                poolSamples++;
                if (pool.pending() > 0) {
                    pendingSamples++;
                }
                peakPending = Math.max(peakPending, pool.pending());
                timeouts += timeoutDelta;
            }
        }

        Totals totals(Duration duration) {
            long iterations = iterations();
            double seconds = duration.toNanos() / 1e9;
            return new Totals(iterations, succeeded.sum(), transientErrors.sum(), errors.sum(),
                    seconds > 0 ? iterations / seconds : 0, peakOpsPerSecond);
        }

        Latency latency() {
            if (histogram.getTotalCount() == 0) {
                return new Latency(0, 0, 0, 0, 0, 0, 0);
            }
            return new Latency(histogram.getTotalCount(),
                    histogram.getMean() / 1000.0,
                    histogram.getValueAtPercentile(50.0) / 1000.0,
                    histogram.getValueAtPercentile(90.0) / 1000.0,
                    histogram.getValueAtPercentile(99.0) / 1000.0,
                    histogram.getValueAtPercentile(99.9) / 1000.0,
                    histogram.getMaxValue() / 1000.0);
        }
    }

    private final int id;

    private final String scenario;

    private final Instant startTime;

    private final Segment total;

    private final List<Segment> phases = new CopyOnWriteArrayList<>();

    // Null before the first phase, then the active phase, then the after phases segment
    private volatile Segment current;

    private volatile Segment afterPhases;

    private final List<CompletableFuture<Worker>> workers = Collections.synchronizedList(new ArrayList<>());

    private final AtomicInteger activeUsers = new AtomicInteger();

    private final Map<String, Integer> droppedUsers = new ConcurrentHashMap<>();

    private final Map<ErrorKey, LongAdder> errors = new ConcurrentHashMap<>();

    // Pool figures, updated by the sampling thread only
    private volatile boolean poolSampled;

    private volatile int poolMaxSize;

    private volatile int peakActiveConnections;

    private volatile int peakPending;

    private volatile long timeouts;

    private volatile long acquireCount;

    private volatile double acquireTotalMillis;

    private volatile double peakAcquireMillis;

    private volatile PoolSample lastPoolSample;

    public Run(int id, String scenario) {
        this(id, scenario, Instant.now());
    }

    Run(int id, String scenario, Instant startTime) {
        this.id = id;
        this.scenario = scenario;
        this.startTime = startTime;
        this.total = new Segment("total", false, startTime);
    }

    public int getId() {
        return id;
    }

    public String getScenario() {
        return scenario;
    }

    /**
     * Marks the start of a phase, ending the previous one.
     */
    public void phaseStarted(Phase phase) {
        phaseStarted(phase, Instant.now());
    }

    void phaseStarted(Phase phase, Instant now) {
        closeCurrent(now);
        Segment segment = new Segment(phase.getName(), phase.isPausePhase(), now);
        segment.peakUsers.set(activeUsers.get());
        phases.add(segment);
        current = segment;
    }

    /**
     * Marks the end of the last phase. Iterations of VUs still running are recorded in
     * an after phases segment.
     */
    public void phasesCompleted() {
        phasesCompleted(Instant.now());
    }

    void phasesCompleted(Instant now) {
        if (afterPhases == null) {
            closeCurrent(now);
            afterPhases = new Segment(AFTER_PHASES, false, now);
            afterPhases.peakUsers.set(activeUsers.get());
            current = afterPhases;
        }
    }

    private void closeCurrent(Instant now) {
        Segment segment = current;
        if (segment != null && segment.endTime == null) {
            segment.endTime = now;
        }
    }

    /**
     * Tracks a VU of this run, created in the current phase.
     *
     * @return the given future
     */
    public CompletableFuture<Worker> addWorker(CompletableFuture<Worker> future) {
        workers.add(future);
        int active = activeUsers.incrementAndGet();
        total.userStarted(active);
        Segment segment = current;
        if (segment != null) {
            segment.userStarted(active);
        }
        future.whenComplete((worker, throwable) -> activeUsers.decrementAndGet());
        return future;
    }

    /**
     * @param droppedUsers the number of VUs not created at max concurrency, by phase name
     */
    public void usersDropped(Map<String, Integer> droppedUsers) {
        this.droppedUsers.putAll(droppedUsers);
    }

    @Override
    public void onIteration(Duration duration, IterationObserver.Outcome outcome, Throwable cause) {
        total.record(duration, outcome);
        Segment segment = current;
        if (segment != null) {
            segment.record(duration, outcome);
        }
        if (cause != null) {
            errors.computeIfAbsent(ErrorKey.of(cause, outcome == IterationObserver.Outcome.TRANSIENT_ERROR),
                            key -> new LongAdder())
                    .increment();
        }
    }

    /**
     * Samples throughput, concurrency and the connection pool, if available. Called once a
     * second by a single thread, starting when the run starts.
     */
    void sample(Instant now, PoolSample pool) {
        long timeoutDelta = 0;

        if (pool != null) {
            PoolSample last = lastPoolSample;
            if (last != null) {
                if (pool.timeoutCount() >= 0 && last.timeoutCount() >= 0) {
                    timeoutDelta = Math.max(0, pool.timeoutCount() - last.timeoutCount());
                }
                if (pool.acquireCount() >= 0 && last.acquireCount() >= 0) {
                    long count = pool.acquireCount() - last.acquireCount();
                    double totalMillis = pool.acquireTotalMillis() - last.acquireTotalMillis();
                    if (count > 0) {
                        acquireCount += count;
                        acquireTotalMillis += totalMillis;
                        peakAcquireMillis = Math.max(peakAcquireMillis, totalMillis / count);
                    }
                }
            }
            lastPoolSample = pool;

            poolSampled = true;
            poolMaxSize = Math.max(poolMaxSize, pool.maxSize());
            peakActiveConnections = Math.max(peakActiveConnections, pool.active());
            peakPending = Math.max(peakPending, pool.pending());
            timeouts += timeoutDelta;
        }

        int active = activeUsers.get();
        total.sample(now, active, pool, timeoutDelta);
        Segment segment = current;
        if (segment != null) {
            segment.sample(now, active, pool, timeoutDelta);
        }
    }

    /**
     * @return a future completed with the VUs of the run once they have all completed, which
     * requires all VUs to have been added
     */
    CompletableFuture<List<Worker>> workersCompleted() {
        List<CompletableFuture<Worker>> futures;
        synchronized (workers) {
            futures = new ArrayList<>(workers);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .handle((unused, throwable) -> futures.stream()
                        .filter(future -> !future.isCompletedExceptionally())
                        .map(CompletableFuture::join)
                        .toList());
    }

    /**
     * @param completedWorkers the VUs of the run, for counting them by status
     * @return the summary of the run ending at the given time, without findings
     */
    public RunSummary toSummary(Instant endTime, RunSummary.Outcome outcome, String outcomeReason,
                                List<Worker> completedWorkers) {
        closeCurrent(endTime);

        List<Segment> segments = new ArrayList<>(phases);
        if (afterPhases != null && afterPhases.iterations() > 0) {
            segments.add(afterPhases);
        }

        List<PhaseSummary> phaseSummaries = segments.stream()
                .map(segment -> {
                    Duration duration = Duration.between(segment.startTime,
                            segment.endTime != null ? segment.endTime : endTime);
                    return new PhaseSummary(segment.name, segment.pause, duration.toMillis(),
                            segment.usersStarted.get(), droppedUsers.getOrDefault(segment.name, 0),
                            segment.peakUsers.get(), segment.totals(duration), segment.latency(),
                            segment.peakPending,
                            segment.poolSamples > 0 ? (double) segment.pendingSamples / segment.poolSamples : 0,
                            segment.timeouts);
                })
                .toList();

        // Throughput over the time VUs were created and running, excluding before steps
        Instant loadStart = phases.isEmpty() ? startTime : phases.getFirst().startTime;
        Duration load = Duration.between(loadStart, endTime);

        RunSummary.Workers workerSummary = new RunSummary.Workers(
                total.usersStarted.get(),
                countWorkers(completedWorkers, WorkerStatus.COMPLETED),
                countWorkers(completedWorkers, WorkerStatus.FAILED),
                countWorkers(completedWorkers, WorkerStatus.CANCELLED),
                total.peakUsers.get());

        RunSummary.Pool pool = poolSampled
                ? new RunSummary.Pool(poolMaxSize, peakActiveConnections, peakPending, timeouts,
                acquireCount > 0 ? acquireTotalMillis / acquireCount : 0, peakAcquireMillis)
                : null;

        List<ErrorSummary> errorSummaries = errors.entrySet().stream()
                .map(entry -> new ErrorSummary(entry.getKey().type(), entry.getKey().sqlState(),
                        entry.getKey().message(), entry.getKey().transientError(), entry.getValue().sum()))
                .sorted(Comparator.comparingLong(ErrorSummary::count).reversed())
                .limit(MAX_ERRORS)
                .toList();

        return new RunSummary(id, scenario, startTime, endTime,
                Duration.between(startTime, endTime).toMillis(), load.toMillis(),
                outcome, outcomeReason, total.totals(load), total.latency(), workerSummary, pool,
                phaseSummaries, errorSummaries, List.of(), encode(total.histogram));
    }

    /**
     * @return a summary of only the outcome of the run, for when it can't be summarized
     */
    RunSummary outcomeSummary(Instant endTime, RunSummary.Outcome outcome, String outcomeReason) {
        long millis = Duration.between(startTime, endTime).toMillis();
        return new RunSummary(id, scenario, startTime, endTime, millis, millis, outcome, outcomeReason,
                new Totals(0, 0, 0, 0, 0, 0), new Latency(0, 0, 0, 0, 0, 0, 0),
                new RunSummary.Workers(0, 0, 0, 0, 0), null, List.of(), List.of(), List.of(), null);
    }

    private static int countWorkers(List<Worker> workers, WorkerStatus status) {
        return (int) workers.stream().filter(worker -> worker.getStatus() == status).count();
    }

    private static String encode(Histogram histogram) {
        ByteBuffer buffer = ByteBuffer.allocate(histogram.getNeededByteBufferCapacity());
        int length = histogram.encodeIntoCompressedByteBuffer(buffer);
        return Base64.getEncoder().encodeToString(Arrays.copyOf(buffer.array(), length));
    }
}
