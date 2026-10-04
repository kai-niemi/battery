package io.battery.web.frontend;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.util.Pair;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.battery.ProfileNames;
import io.battery.web.frontend.model.TopicName;

@Component
@Profile(value = ProfileNames.ONLINE)
public class SimpMessagePublisher {
    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    private final AtomicInteger lostEvents = new AtomicInteger();

    private final AtomicInteger sentEvents = new AtomicInteger();

    private final BlockingQueue<Pair<TopicName, Object>> queue = new LinkedBlockingQueue<>(128);

    @Scheduled(fixedRate = 1000, initialDelay = 3000, timeUnit = TimeUnit.MILLISECONDS)
    public void drainSendQueue() {
        Pair<TopicName, Object> event = queue.poll();
        while (event != null) {
            simpMessagingTemplate.convertAndSend(event.getFirst().getValue(), event.getSecond());
            event = queue.poll();
        }
    }

    public <T> void convertAndSend(TopicName topic, T payload) {
        if (queue.offer(Pair.of(topic, payload != null ? payload : ""))) {
            sentEvents.incrementAndGet();
        } else {
            lostEvents.incrementAndGet();
        }
    }
}
