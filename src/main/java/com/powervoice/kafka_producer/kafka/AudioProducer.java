package com.powervoice.kafka_producer.kafka;

import com.powervoice.kafka_producer.dto.CallItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.kafka.support.SendResult;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioProducer {

    private static final String TOPIC = "Reqdata"; // 케이스 주의
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    public void send(CallItem item, byte[] audio) {
        if (audio == null || audio.length == 0)
            throw new IllegalArgumentException("audio empty for CALL_ID=" + item.getCallId());

        ProducerRecord<String, byte[]> record =
                new ProducerRecord<>(TOPIC, item.getCallId(), audio);

        // 메타데이터를 헤더로 첨부
        add(record, "ANI", item.getAni());
        add(record, "CUSTM_TYPE", item.getCustmType());
        add(record, "CALL_TYPE", item.getCallType());
        add(record, "EXT", item.getExt());
        add(record, "FILE_PATH", item.getFilePath());
        add(record, "START_TIME", item.getStartTime());
        add(record, "END_SEC", item.getEndTime());
        add(record, "DURATION", item.getDuration());
        add(record, "REQ_TIME", item.getReqTime());

        kafkaTemplate.send(record)
                .whenComplete((SendResult<String, byte[]> result, Throwable ex) -> {
                    if (ex != null) {
                        log.error("Kafka send fail CALL_ID={}, path={}", item.getCallId(), item.getFilePath(), ex);
                    } else if (result != null) {
                        RecordMetadata m = result.getRecordMetadata();
                        log.info("Kafka sent topic={}, partition={}, offset={}, key={}",
                                m.topic(), m.partition(), m.offset(), item.getCallId());
                    }
                });
    }

    private void add(ProducerRecord<String, byte[]> r, String k, Object v){
        if (v == null) return;
        r.headers().add(k, String.valueOf(v).getBytes(StandardCharsets.UTF_8));
    }
}
