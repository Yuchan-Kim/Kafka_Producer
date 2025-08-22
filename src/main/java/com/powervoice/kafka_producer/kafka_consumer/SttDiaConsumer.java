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
        log.info("[SttDia] {}", message);
    }
}

