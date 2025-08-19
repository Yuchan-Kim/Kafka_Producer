package com.powervoice.kafka_producer.kafka_consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SummaryConsumer {
    @KafkaListener(topics = "Summary", groupId = "log-group")
    public void listen(String message) {
        log.info("[Summary] {}", message);
    }
}
