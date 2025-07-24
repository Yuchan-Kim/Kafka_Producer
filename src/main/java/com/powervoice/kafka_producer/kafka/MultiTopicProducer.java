package com.powervoice.kafka_producer.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MultiTopicProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendReqdata(String message) {
        kafkaTemplate.send("reqdata", message);
    }


}
