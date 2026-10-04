package io.battery.web.frontend;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;

import io.battery.ProfileNames;
import io.battery.web.frontend.model.TopicName;

@WebController
@Profile(value = ProfileNames.ONLINE)
public class ChartsController {
    @Autowired
    private SimpMessagePublisher simpMessagePublisher;

    @Scheduled(fixedRate = 5, initialDelay = 5, timeUnit = TimeUnit.SECONDS)
    public void chartUpdate() {
        simpMessagePublisher.convertAndSend(TopicName.SYSTEM_CHARTS_UPDATE, null);
        simpMessagePublisher.convertAndSend(TopicName.WORKER_CHARTS_UPDATE, null);
    }

    @GetMapping("/system-charts")
    public Callable<String> indexPage() {
        return () -> "system-charts";
    }

    @GetMapping("/worker-charts")
    public Callable<String> metricsPage() {
        return () -> "worker-charts";
    }
}
