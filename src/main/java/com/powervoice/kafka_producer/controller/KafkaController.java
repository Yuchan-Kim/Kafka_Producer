// src/main/java/com/powervoice/kafka_producer/controller/KafkaController.java
package com.powervoice.kafka_producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.BatchRequestDTO;
import com.powervoice.kafka_producer.dto.CallDataDTO;
import com.powervoice.kafka_producer.queue.MessageQ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/reqdata")
@RequiredArgsConstructor
@Slf4j
public class KafkaController {
    private final ObjectMapper objectMapper;
    private final MessageQ messageQ;

    @PostMapping
    public ResponseEntity<Map<String,Object>> process(@RequestBody JsonNode node) {
        log.info("[KafkaController.process] : Received request: {}", node);
        try {
            if (node.has("CALL_ARR") && node.get("CALL_ARR").isArray()) {
                // 배치 처리
                BatchRequestDTO batch = objectMapper.treeToValue(node, BatchRequestDTO.class);

                Optional<String> err = validateBatch(batch);
                if (err.isPresent()) {
                    log.warn("[KafkaController.process] : Validation failed: {}", err.get());
                    return bad(err.get());
                }

                log.info("[KafkaController.process] : Batch validated, queuing {} items", batch.getCALL_ARR().size());
                batch.getCALL_ARR().forEach(messageQ::add);
                log.info("[KafkaController.process] : Queued {} items", batch.getCALL_ARR().size());

                return ok(batch.getCALL_ARR().size());

            } else {
                // 단일 처리
                CallDataDTO single = objectMapper.treeToValue(node, CallDataDTO.class);

                Optional<String> err = validateSingle(single);
                if (err.isPresent()) {
                    log.warn("[KafkaController.process] : Validation failed: {}", err.get());
                    return bad(err.get());
                }

                log.info("[KafkaController.process] : Single validated, queuing callId={}", single.getCallId());
                messageQ.add(single);
                log.info("[KafkaController.process] : Queued callId={}", single.getCallId());

                return ok(1);
            }
        } catch (JsonProcessingException e) {
            log.error("[KafkaController.process] : Invalid JSON: {}", e.getOriginalMessage());
            return bad("Invalid JSON: " + e.getOriginalMessage());
        }
    }

    // 누락 데이터 체크 메소드
    private Optional<String> validateBatch(BatchRequestDTO b) {
        if (b.getCALL_ARR() == null || b.getCALL_ARR().isEmpty()) {
            return Optional.of("[KafkaController.validateBatch] CALL_ARR must not be empty");
        }
        if (b.getTOTAL() != b.getCALL_ARR().size()) {
            return Optional.of("[KafkaController.validateBatch] TOTAL and CALL_ARR size mismatch");
        }
        return Optional.empty();
    }


    // 누락 데이터 체크 메소드
    private Optional<String> validateSingle(CallDataDTO c) {
        if (c.getCallId() == null || c.getCallId().isEmpty()) {
            return Optional.of("[KafkaController.validateSingle] CALL_ID is required");
        }
        if (c.getFilePath() == null || c.getFilePath().isEmpty()) {
            return Optional.of("[KafkaController.validateSingle] FILE_PATH is required");
        }
        return Optional.empty();
    }

    // 성공 응답 메소드
    private ResponseEntity<Map<String,Object>> ok(int queued) {
        log.info("[KafkaController.ResponseEntity] : SUCCESS queued {}", queued);
        return ResponseEntity.ok(Map.of("status","SUCCESS", "queued", queued));
    }

    // 실패 응답 메소드
    private ResponseEntity<Map<String,Object>> bad(String msg) {
        log.warn("[KafkaController.ResponseEntity] : FAIL {}", msg);
        return ResponseEntity.badRequest().body(Map.of("status","FAIL", "errorMsg", msg));
    }
}
