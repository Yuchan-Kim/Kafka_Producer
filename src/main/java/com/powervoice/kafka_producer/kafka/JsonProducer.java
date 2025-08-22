package com.powervoice.kafka_producer.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;


@Service
@RequiredArgsConstructor
@Slf4j
public class JsonProducer {


    private static final String TOPIC = "Reqdata";
    private final KafkaTemplate<String, String> kafkaTemplate;


    public void sendSingle(String key, String rawJson) {
        ProducerRecord<String, String> r = new ProducerRecord<>(TOPIC,rawJson);
        r.headers().add("content-type", "application/json".getBytes(StandardCharsets.UTF_8));


        kafkaTemplate.send(r).whenComplete((SendResult<String, String> result, Throwable ex) -> {
            if (ex != null) {
                log.error("Kafka send fail key={}", key, ex);
            } else if (result != null) {
                RecordMetadata m = result.getRecordMetadata();
                log.info("Kafka sent topic={}, partition={}, offset={}, key={}",
                        m.topic(), m.partition(), m.offset(), key);
            }
        });
    }
}