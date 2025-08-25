package com.powervoice.kafka_producer.kafka_consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.powervoice.kafka_producer.session.CallSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryConsumer {

    @Value("${app.target_add}")
    private String targetAdd;

    @Value("${app.debug.enabled}")
    private boolean debugEnabled;

    @Value("${app.debug.auto_rep}")
    private boolean autoRep;

    private final CallSessionRegistry registry;
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "Summary", groupId = "log-group")
    public void listen(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String callId = root.at("/key/callId").asText();
            JsonNode reqReturn = root.get("Reqreturn");


            if (callId == null || reqReturn == null) {
                log.warn("[SummaryConsumer] 필수 필드 누락: callId={}, reqReturn={}", callId, reqReturn);
                return;
            }

            // 전송 대상 주소 결정 (autoRep=true면 매핑된 IP, false면 targetAdd)
            String address;
            if (autoRep) {
                address = registry.getClientIp(callId);
                if (address == null || address.isBlank()) {
                    log.warn("[SummaryConsumer] autoRep=true 인데 callId={} 에 대한 IP 매핑이 없습니다.", callId);
                    return;
                }
            } else {
                address = targetAdd;
            }

            MediaType JSON_UTF8 = MediaType.valueOf("application/json;charset=UTF-8");
            String url = "http://" + address;
            ObjectNode sendBody;
            if (debugEnabled) {
                // 전체 메시지 전송
                sendBody = objectMapper.createObjectNode();
                sendBody.setAll((ObjectNode) root);  // 전체 메시지 복사
            } else {
                // Reqreturn만 전송
                sendBody = objectMapper.createObjectNode();
                sendBody.set("Reqreturn", reqReturn);
            }

            webClient.post()
                    .uri(url)
                    .contentType(JSON_UTF8)
                    .bodyValue(sendBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))  // 네트워크 타임아웃
                    .doOnError(e -> log.error("[SummaryConsumer] 전송 실패: callId={}, url={}, 이유={}", callId, url, e.toString()))
                    .subscribe(
                            resp -> {
                                log.info("[SummaryConsumer] callId={} 응답 수신 → {}", callId, resp);
                                try {
                                    registry.unregister(callId);
                                } catch (Exception ex) {
                                    log.warn("[SummaryConsumer] unregister 실패: callId={}, err={}", callId, ex.getMessage());
                                }
                            },
                            err -> log.warn("[SummaryConsumer] subscribe 처리 중 예외 발생: callId={}, err={}", callId, err.toString())
                    );


        } catch (Exception e) {
            log.error("[SummaryConsumer] 처리 중 예외", e);
        }
    }
}
