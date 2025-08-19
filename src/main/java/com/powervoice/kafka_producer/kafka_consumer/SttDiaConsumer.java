package com.powervoice.kafka_producer.kafka_consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.powervoice.kafka_producer.session.CallSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class SttDiaConsumer {

    private final CallSessionRegistry registry;
    private final WebClient webClient = WebClient.create();

    @KafkaListener(topics = "SttDia", groupId = "stt-group", containerFactory = "kafkaListenerContainerFactory")
    public void listen(String message) {
        try {
            JsonNode json = new ObjectMapper().readTree(message);
            String callId = json.get("callId").asText();
            String empNo = json.get("empNo").asText();
            String startTime = json.get("startTime").asText();
            String endTime = json.get("endTime").asText();
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = mapper.createObjectNode();
            payload.put("callId", callId);
            payload.put("empNo", empNo);
            payload.put("startTime", startTime);
            payload.put("endTime", endTime);
            String clientIp = registry.getClientIp(callId);
            if (clientIp == null) {
                log.warn("❌ 등록되지 않은 callId: {}", callId);
                return;
            }

            String targetUrl = "http://" + clientIp + ":port/your-endpoint"; // 포트/엔드포인트 설정 필요
            log.info("➡️ {}로 STT 결과 전송 중...", targetUrl);

            webClient.post()
                    .uri(targetUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(resp -> log.info("✅ 전송 성공"))
                    .doOnError(err -> log.error("❌ 전송 실패", err))
                    .subscribe();

        } catch (Exception e) {
            log.error("❌ STT 메시지 처리 실패", e);
        }
    }
}

