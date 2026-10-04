package io.battery.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.util.Pair;

import io.battery.metrics.TimeSeries;
import io.battery.scenario.worker.WorkTracker;
import io.battery.scenario.worker.Worker;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Configuration
public class InstrumentationConfig {
    @Bean
    public TimeSeries connectionPoolTimingTimeSeries(@Autowired MeterRegistry registry) {
        // The time spent waiting for the pool is part of the measured call latency, but the
        // acquire timer's cumulative count and total don't chart next to connection counts
        Gauge.builder("battery.connections.acquire.max", registry, value -> {
                    Timer timer = value.find("hikaricp.connections.acquire")
                            .tag("pool", "battery-pool").timer();
                    return timer != null ? timer.max(TimeUnit.MILLISECONDS) : 0;
                })
                .description("Max connection acquire time (ms)")
                .baseUnit("milliseconds")
                .register(registry);

        return new TimeSeries("connection-pool-timings", registry, () -> List.of(
                Pair.of("battery.connections.acquire.max",
                        registry.find("battery.connections.acquire.max"))
        ));
    }

    @Bean
    public TimeSeries connectionPoolTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries("connection-pool", registry, () -> List.of(
                Pair.of("hikaricp.connections.active?pool=battery-pool",
                        registry.find("hikaricp.connections.active").tag("pool", "battery-pool")),
                Pair.of("hikaricp.connections.idle?pool=battery-pool",
                        registry.find("hikaricp.connections.idle").tag("pool", "battery-pool")),
                Pair.of("hikaricp.connections.min?pool=battery-pool",
                        registry.find("hikaricp.connections.min").tag("pool", "battery-pool")),
                Pair.of("hikaricp.connections.max?pool=battery-pool",
                        registry.find("hikaricp.connections.max").tag("pool", "battery-pool")),
                // Threads waiting for a connection, typically VUs in excess of the pool size
                Pair.of("hikaricp.connections.pending?pool=battery-pool",
                        registry.find("hikaricp.connections.pending").tag("pool", "battery-pool")),
                Pair.of("hikaricp.connections.timeout?pool=battery-pool",
                        registry.find("hikaricp.connections.timeout").tag("pool", "battery-pool"))
        ));
    }

    @Bean
    public TimeSeries threadPoolTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries("threads", registry, () -> List.of(
                Pair.of("jvm.threads.live", registry.find("jvm.threads.live")),
                Pair.of("jvm.threads.peak", registry.find("jvm.threads.peak"))
        ));
    }

    @Bean
    public TimeSeries cpuTimeSeries(@Autowired MeterRegistry registry) {
        return new TimeSeries("system", registry, () -> List.of(
                Pair.of("process.cpu.usage", registry.find("process.cpu.usage")),
                Pair.of("system.cpu.usage", registry.find("system.cpu.usage"))
        ));
    }

    @Bean
    public TimeSeries workerTimeSeries(
            @Autowired WorkTracker workTracker,
            @Autowired MeterRegistry registry) {
        Gauge.builder("battery.workers.total", workTracker, value ->
                        value.listWorkers().size())
                .description("Total amount of workers")
                .baseUnit("workers")
                .register(registry);

        Gauge.builder("battery.workers.active", workTracker, value ->
                        value.listWorkers(Worker::isRunning).size())
                .description("Active workers not finished, failed or cancelled")
                .register(registry);

        Gauge.builder("battery.workers.cancelled", workTracker, value ->
                        value.listWorkers(Worker::isCancelled).size())
                .description("Active workers cancelled")
                .register(registry);

        Gauge.builder("battery.workers.errors", workTracker, value ->
                        value.listWorkers(Worker::hasProblems).size())
                .description("Workers with at least one error")
                .register(registry);

        return new TimeSeries("workers", registry, () -> List.of(
                Pair.of("battery.workers.total", registry.find("battery.workers.total")),
                Pair.of("battery.workers.active", registry.find("battery.workers.active")),
                Pair.of("battery.workers.errors", registry.find("battery.workers.errors"))
        ));
    }
}
