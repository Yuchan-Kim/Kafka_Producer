package com.powervoice.kafka_producer.kafka_consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.powervoice.kafka_producer.session.CallSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SummaryConsumer {
    @Value("${app.target-port}")
    private String targetPort;

    @Value("${app.target-ip}")
    private String targetIp;

    @Value("${app.debug.enabled}")
    private boolean debugEnabled;


    //private final CallSessionRegistry callSessionRegistry;
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "Summary", groupId = "log-group")
    public void listen(String message) {

        if (!debugEnabled) {
            try {
                JsonNode root = objectMapper.readTree(message);

                String callId = root.at("/key/callId").asText();
                JsonNode reqReturn = root.get("Reqreturn");
                JsonNode sttDia = root.get("stt-dia");

                if (callId == null || reqReturn == null || sttDia == null) {
                    log.warn("필수 필드 누락: callId={}, reqReturn={}, sttDia={}", callId, reqReturn, sttDia);
                    return;
                }


                if (targetIp == null) {
                    log.warn("IP 매핑 없음: callId={}", callId);
                    return;
                }

                ObjectNode sendBody = objectMapper.createObjectNode();

                sendBody.set("Reqreturn", reqReturn);
                MediaType JSON_UTF8 = MediaType.valueOf("application/json;charset=UTF-8");

                String url = "http://" + targetIp + ":" + targetPort; // 예: http://192.168.0.87:9382
                webClient.post()
                        .uri(url) // ← 반드시 스킴 포함
                        .contentType(JSON_UTF8)
                        .bodyValue(sendBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(ex -> log.error("POST 전송 실패: callId={}, url={}, err={}", callId, url, ex.toString()))
                        .subscribe(resp -> log.info("POST 전송 완료: callId={}, 응답={}", callId, resp));

            } catch (Exception e) {
                log.error("SummaryConsumer 처리 중 예외", e);
            }
        } else {

            try {
                JsonNode root = objectMapper.readTree(message);

                String callId = root.at("/key/callId").asText();
                JsonNode reqReturn = root.get("Reqreturn");
                JsonNode sttDia = root.get("stt-dia");

                if (callId == null || reqReturn == null || sttDia == null) {
                    log.warn("필수 필드 누락: callId={}, reqReturn={}, sttDia={}", callId, reqReturn, sttDia);
                    return;
                }


                if (targetIp == null) {
                    log.warn("IP 매핑 없음: callId={}", callId);
                    return;
                }

                ObjectNode sendBody = objectMapper.createObjectNode();
                sendBody.set("stt-dia", sttDia);
                sendBody.set("Reqreturn", reqReturn);
                MediaType JSON_UTF8 = MediaType.valueOf("application/json;charset=UTF-8");

                String url = "http://" + targetIp + ":" + targetPort; // 예: http://192.168.0.87:9382
                webClient.post()
                        .uri(url) // ← 반드시 스킴 포함
                        .contentType(JSON_UTF8)
                        .bodyValue(sendBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .doOnError(ex -> log.error("POST 전송 실패: callId={}, url={}, err={}", callId, url, ex.toString()))
                        .subscribe(resp -> log.info("POST 전송 완료: callId={}, 응답={}", callId, resp));

            } catch (Exception e) {
                log.error("SummaryConsumer 처리 중 예외", e);
            }
        }
    }
}

