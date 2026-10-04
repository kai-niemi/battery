package io.battery.scenario;

import java.time.Duration;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;

import io.battery.model.BatteryModel;
import io.battery.model.Phase;

@Tag("unit-test")
public class ConnectionPoolSizerTest {
    private static Phase rampingPhase(String name, int maxConcurrency) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setDuration(Duration.ofSeconds(30));
        phase.setStartRate(4);
        phase.setMaxRate(30);
        phase.setMaxConcurrency(maxConcurrency);
        return phase;
    }

    private static Phase fixedPhase(String name, int users) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setDuration(Duration.ofSeconds(30));
        phase.setUsers(users);
        return phase;
    }

    private static Phase pausePhase(String name) {
        Phase phase = new Phase();
        phase.setName(name);
        phase.setDuration(Duration.ofSeconds(30));
        return phase;
    }

    private static HikariConfig pool(int maxSize, int minIdle) {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(maxSize);
        config.setMinimumIdle(minIdle);
        return config;
    }

    private static BatteryModel model(Phase... phases) {
        BatteryModel batteryModel = new BatteryModel();
        batteryModel.setPhases(List.of(phases));
        return batteryModel;
    }

    @Test
    public void givenPhases_expectConcurrencyBounds() {
        Assertions.assertThat(ConnectionPoolSizer.concurrencyOf(rampingPhase("r", 50))).isEqualTo(50);
        Assertions.assertThat(ConnectionPoolSizer.concurrencyOf(rampingPhase("r", 0))).isEqualTo(Integer.MAX_VALUE);
        Assertions.assertThat(ConnectionPoolSizer.concurrencyOf(fixedPhase("f", 20))).isEqualTo(20);
        Assertions.assertThat(ConnectionPoolSizer.concurrencyOf(pausePhase("p"))).isZero();
    }

    @Test
    public void givenPhasesWithinPoolLimit_expectNoWarnings() {
        BatteryModel batteryModel = model(fixedPhase("warm up", 20), pausePhase("steady"), rampingPhase("peak", 64));

        Assertions.assertThat(new ConnectionPoolSizer(pool(32, 32), batteryModel).concurrencyWarnings()).isEmpty();
    }

    @Test
    public void givenPhasesBeyondPoolLimit_expectWarnings() {
        BatteryModel batteryModel = model(rampingPhase("peak", 100), rampingPhase("unlimited", 0),
                fixedPhase("crowd", 80));

        List<String> warnings = new ConnectionPoolSizer(pool(32, 32), batteryModel).concurrencyWarnings();

        Assertions.assertThat(warnings).hasSize(3);
        Assertions.assertThat(warnings.get(0))
                .startsWith("Phase [peak] allows up to 100 concurrent users")
                .contains("limited to 64 connections", "battery.connectionPool.maxSize");
        Assertions.assertThat(warnings.get(1)).startsWith("Phase [unlimited] has no maxConcurrency limit");
        Assertions.assertThat(warnings.get(2)).startsWith("Phase [crowd] creates 80 users");
    }

    @Test
    public void givenAutoSizeDisabled_expectWarningAgainstConfiguredPoolSize() {
        BatteryModel batteryModel = model(rampingPhase("peak", 40));
        batteryModel.getConnectionPool().setAutoSize(false);

        Assertions.assertThat(new ConnectionPoolSizer(pool(32, 32), batteryModel).concurrencyWarnings())
                .singleElement().asString()
                .contains("limited to 32 connections", "spring.datasource.hikari.maximum-pool-size");
    }

    @Test
    public void givenLaunch_expectPoolGrownUntilLeaseClosed() {
        HikariConfig pool = pool(32, 32);
        BatteryModel batteryModel = model(fixedPhase("warm up", 20), rampingPhase("peak", 50));
        ConnectionPoolSizer sizer = new ConnectionPoolSizer(pool, batteryModel);

        ConnectionPoolSizer.Lease lease = sizer.resizeFor(batteryModel.getPhases());
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(50);
        Assertions.assertThat(pool.getMinimumIdle()).isEqualTo(50);

        lease.close();
        lease.close();
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(32);
        Assertions.assertThat(pool.getMinimumIdle()).isEqualTo(32);
    }

    @Test
    public void givenUnlimitedPhase_expectPoolGrownToMaxSize() {
        HikariConfig pool = pool(32, 32);
        BatteryModel batteryModel = model(rampingPhase("unlimited", 0));

        new ConnectionPoolSizer(pool, batteryModel).resizeFor(batteryModel.getPhases());

        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(64);
    }

    @Test
    public void givenLowConcurrency_expectPoolNotShrunk() {
        HikariConfig pool = pool(32, 32);
        BatteryModel batteryModel = model(fixedPhase("small", 10));

        new ConnectionPoolSizer(pool, batteryModel).resizeFor(batteryModel.getPhases());

        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(32);
    }

    @Test
    public void givenNonFixedPool_expectMinIdleKept() {
        HikariConfig pool = pool(32, 4);
        BatteryModel batteryModel = model(rampingPhase("peak", 50));

        ConnectionPoolSizer.Lease lease = new ConnectionPoolSizer(pool, batteryModel)
                .resizeFor(batteryModel.getPhases());
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(50);
        Assertions.assertThat(pool.getMinimumIdle()).isEqualTo(4);

        lease.close();
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(32);
        Assertions.assertThat(pool.getMinimumIdle()).isEqualTo(4);
    }

    @Test
    public void givenOverlappingLaunches_expectPoolSizedForLargestUntilAllClosed() {
        HikariConfig pool = pool(32, 32);
        BatteryModel batteryModel = model(rampingPhase("peak", 64));
        ConnectionPoolSizer sizer = new ConnectionPoolSizer(pool, batteryModel);

        ConnectionPoolSizer.Lease large = sizer.resizeFor(List.of(rampingPhase("large", 60)));
        ConnectionPoolSizer.Lease small = sizer.resizeFor(List.of(rampingPhase("small", 40)));
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(60);

        large.close();
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(40);

        small.close();
        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(32);
    }

    @Test
    public void givenAutoSizeDisabled_expectPoolUnchanged() {
        HikariConfig pool = pool(32, 32);
        BatteryModel batteryModel = model(rampingPhase("peak", 50));
        batteryModel.getConnectionPool().setAutoSize(false);

        new ConnectionPoolSizer(pool, batteryModel).resizeFor(batteryModel.getPhases()).close();

        Assertions.assertThat(pool.getMaximumPoolSize()).isEqualTo(32);
    }
}
