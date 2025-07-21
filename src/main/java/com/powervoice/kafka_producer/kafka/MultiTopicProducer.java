package com.powervoice.kafka_producer.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultiTopicProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendToCallTopic(String message) {
        kafkaTemplate.send("topic-call", message);
    }

    public void sendToMaskTopic(String message) {
        kafkaTemplate.send("topic-mask", message);
    }

    public void sendToResultTopic(String message) {
        kafkaTemplate.send("topic-result", message);
    }

    public void sendToNotifyTopic(String message) {
        kafkaTemplate.send("topic-notify", message);
    }
}
