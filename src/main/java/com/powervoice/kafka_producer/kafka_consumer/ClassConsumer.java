package com.powervoice.kafka_producer.kafka_consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.session.CallSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClassConsumer {

    @Value("${app.debug.enabled}")
    private boolean debugEnabled;

    @Value("${app.debug.auto_rep}")
    private boolean autoRep;

    @Value("${app.target_add}")
    private String targetAdd;

    private final WebClient webClient = WebClient.create();
    private final CallSessionRegistry registry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "Class", groupId = "log-group")
    public void listen(String message) {
        log.info("[ClassConsumer] Received Kafka message: {}", message);

        // 디버그 모드가 비활성화된 경우 로그 출력만 진행
        if (!debugEnabled) return;


        try {
            JsonNode json = objectMapper.readTree(message);
            String callId = json.at("/key/callId").asText();  // 중첩 구조 안전하게 파싱

            
            
            if (callId == null || callId.isBlank()) {
                log.error("[ClassConsumer] callId가 누락됨 → 원본 message={}", message);
                return;
            }


            // 전송 대상 주소 결정 (autoRep=true면 매핑된 IP, false면 targetAdd)
            String address;
            if (autoRep) {
                address = registry.getClientIp(callId);
                if (address == null || address.isBlank()) {
                    log.error("[ClassConsumer] autoRep=true 인데 callId={} 에 대한 IP 매핑이 없습니다.", callId);
                    return;
                }
            } else {
                address = targetAdd;
            }


            MediaType JSON_UTF8 = MediaType.valueOf("application/json;charset=UTF-8");

            String url = "http://" + address;
            webClient.post()
                    .uri(url)
                    .contentType(JSON_UTF8)
                    .bodyValue(message)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))  // 네트워크 타임아웃
                    .doOnError(e -> log.error("[ClassConsumer] 전송 실패: callId={}, url={}, 이유={}", callId, url, e.toString()))
                    .subscribe(
                            resp -> {
                                log.info("[ClassConsumer] callId={} 응답 수신 → {}", callId, resp);
                                try {
                                    registry.unregister(callId);
                                } catch (Exception ex) {
                                    log.warn("[ClassConsumer] unregister 실패: callId={}, err={}", callId, ex.getMessage());
                                }
                            },
                            err -> log.warn("[ClassConsumer] subscribe 처리 중 예외 발생: callId={}, err={}", callId, err.toString())
                    );

        } catch (Exception e) {
            log.error("[ClassConsumer] 처리 중 예외", e);
        }
    }
}
