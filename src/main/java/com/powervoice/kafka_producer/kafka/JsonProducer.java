package com.powervoice.kafka_producer.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonProducer {

    private static final String TOPIC = "reqdata"; // 소문자 일관 권장
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper om;

    public void sendRawJson(String rawJson) throws Exception {
        JsonNode root = om.readTree(rawJson);
        JsonNode arr = root.path("CALL_ARR");
        String key = (arr.isArray() && arr.size() > 0)
                ? arr.get(0).path("CALL_ID").asText(null)
                : null;

        ProducerRecord<String, String> r = new ProducerRecord<>(TOPIC, key, rawJson);
        // 대표 헤더 1개
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
