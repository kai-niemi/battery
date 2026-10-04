package io.battery.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariConfigMXBean;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.annotation.PostConstruct;

import io.battery.model.BatteryModel;
import io.battery.model.ConnectionPool;
import io.battery.model.Phase;

/**
 * Sizes the connection pool to the concurrency of scenario launches. A virtual user (VU)
 * holds a connection while it runs SQL, so with fewer connections than concurrent VUs the
 * rest queue for connections, which inflates their measured latency and may time out.
 * <p>
 * With auto sizing enabled, each launch grows the pool to the highest concurrency its phases
 * allow, up to the {@link ConnectionPool#getMaxSize() max size}, until all of its VUs have
 * completed. Concurrent launches share the pool, which is sized for the largest of them. A
 * fixed size pool stays fixed size, so that it fills up in the background rather than as
 * VUs arrive.
 * <p>
 * On startup, warns about phases whose concurrency can exceed what the pool can provide.
 */
@Component
public class ConnectionPoolSizer {
    /**
     * Restores the pool size once the launch it was sized for has completed.
     */
    public interface Lease extends AutoCloseable {
        @Override
        void close();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final HikariConfigMXBean pool;

    private final BatteryModel batteryModel;

    // Pool sizes required by the launches in progress, guarding the fields below
    private final List<Integer> leasedSizes = new ArrayList<>();

    // Pool settings before the first launch in progress, restored after the last one
    private int baseMaxSize;

    private int baseMinIdle;

    @Autowired
    public ConnectionPoolSizer(HikariDataSource dataSource, BatteryModel batteryModel) {
        this(dataSource.getHikariConfigMXBean(), batteryModel);
    }

    ConnectionPoolSizer(HikariConfigMXBean pool, BatteryModel batteryModel) {
        this.pool = pool;
        this.batteryModel = batteryModel;
    }

    @PostConstruct
    public void init() {
        concurrencyWarnings().forEach(logger::warn);
    }

    /**
     * @return the highest number of concurrent VUs the phase allows, ignoring VUs still
     * running from earlier phases, or {@link Integer#MAX_VALUE} if unlimited
     */
    static int concurrencyOf(Phase phase) {
        if (phase.isPausePhase()) {
            return 0;
        }
        if (phase.getMaxConcurrency() > 0) {
            return phase.getMaxConcurrency();
        }
        return phase.isFixedPhase() ? phase.getUsers() : Integer.MAX_VALUE;
    }

    private int largestPoolSize(int maxSize) {
        ConnectionPool settings = batteryModel.getConnectionPool();
        return settings.isAutoSize() ? Math.max(maxSize, settings.getMaxSize()) : maxSize;
    }

    List<String> concurrencyWarnings() {
        final int limit = largestPoolSize(pool.getMaximumPoolSize());
        final String remedy = batteryModel.getConnectionPool().isAutoSize()
                ? "battery.connectionPool.maxSize"
                : "spring.datasource.hikari.maximum-pool-size";

        return batteryModel.getPhases().stream()
                .filter(phase -> concurrencyOf(phase) > limit)
                .map(phase -> {
                    String concurrency = phase.getMaxConcurrency() > 0
                            ? "allows up to %d concurrent users".formatted(phase.getMaxConcurrency())
                            : phase.isFixedPhase()
                            ? "creates %d users".formatted(phase.getUsers())
                            : "has no maxConcurrency limit";
                    return ("Phase [%s] %s, but the connection pool is limited to %d connections so the "
                            + "remaining users queue for connections. Lower maxConcurrency, or raise %s "
                            + "within the database connection limit.")
                            .formatted(phase.getName(), concurrency, limit, remedy);
                })
                .toList();
    }

    /**
     * Grows the pool for a launch of the given phases, if auto sizing is enabled.
     *
     * @param phases the phases of the launch
     * @return a lease to close once all VUs of the launch have completed
     */
    public Lease resizeFor(List<Phase> phases) {
        if (!batteryModel.getConnectionPool().isAutoSize()) {
            return () -> {
            };
        }

        final Integer size;
        synchronized (leasedSizes) {
            if (leasedSizes.isEmpty()) {
                baseMaxSize = pool.getMaximumPoolSize();
                baseMinIdle = pool.getMinimumIdle();
            }
            int peak = phases.stream().mapToInt(ConnectionPoolSizer::concurrencyOf).max().orElse(0);
            size = Math.max(baseMaxSize, Math.min(peak, largestPoolSize(baseMaxSize)));
            leasedSizes.add(size);
            applyPoolSize();
        }

        final AtomicBoolean closed = new AtomicBoolean();
        return () -> {
            if (closed.compareAndSet(false, true)) {
                synchronized (leasedSizes) {
                    leasedSizes.remove(size);
                    applyPoolSize();
                }
            }
        };
    }

    // Called with leasedSizes locked
    private void applyPoolSize() {
        final int size = Math.max(baseMaxSize,
                leasedSizes.stream().mapToInt(Integer::intValue).max().orElse(baseMaxSize));
        // A negative min idle means unset, which Hikari treats as a fixed size pool
        final boolean fixedSize = baseMinIdle < 0 || baseMinIdle >= baseMaxSize;
        final int minIdle = fixedSize ? size : baseMinIdle;

        final int current = pool.getMaximumPoolSize();
        if (size == current && minIdle == pool.getMinimumIdle()) {
            return;
        }

        // Keep min idle within max size while resizing
        if (size >= current) {
            pool.setMaximumPoolSize(size);
            pool.setMinimumIdle(minIdle);
        } else {
            pool.setMinimumIdle(minIdle);
            pool.setMaximumPoolSize(size);
        }

        logger.info("Resized connection pool from %d to %d connections (min idle %d)"
                .formatted(current, size, minIdle));
    }
}
