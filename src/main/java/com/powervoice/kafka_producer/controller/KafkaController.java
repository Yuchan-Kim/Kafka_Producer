package com.powervoice.kafka_producer.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.BatchReq;
import com.powervoice.kafka_producer.dto.CallItem;
import com.powervoice.kafka_producer.queue.MessageQ;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reqdata")
@RequiredArgsConstructor
@Slf4j
public class KafkaController {

    private final ObjectMapper om;
    private final MessageQ messageQ;

    @Value("${app.queue.offer-timeout-ms:200}")
    private long offerTimeoutMs;

    @PostMapping
    public ResponseEntity<Map<String,Object>> process(@RequestBody JsonNode node) {
        try {
            BatchReq req = om.treeToValue(node, BatchReq.class);
            String err = validate(req);
            if (err != null) return bad(err);

            int enq = 0;
            for (CallItem item : req.getCallArr()) {
                boolean ok = messageQ.offer(item, offerTimeoutMs);
                if (!ok) return tooMany("QUEUE_FULL");
                enq++;
            }
            log.info("Enqueued {} items (queue size={}, remain={})",
                    enq, messageQ.size(), messageQ.remainingCapacity());
            return ok(enq);

        } catch (JsonProcessingException e) {
            log.error("Invalid JSON: {}", e.getOriginalMessage());
            return bad("Invalid JSON: " + e.getOriginalMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return bad("INTERRUPTED");
        }
    }

    // ---- validators ----
    private String validate(BatchReq r){
        if (r.getCMD() == null || r.getCMD().isBlank())
            return "CMD required";
        if (!List.of("BATCH_PROC","SINGLE_PROC").contains(r.getCMD()))
            return "CMD must be BATCH_PROC or SINGLE_PROC";

        if (r.getTOTAL() == null || r.getTOTAL() < 1)
            return "TOTAL required (>0)";
        if (r.getCallArr() == null || r.getCallArr().isEmpty())
            return "CALL_ARR required";
        if (r.getTOTAL() != r.getCallArr().size())
            return "TOTAL mismatch";

        if ("SINGLE_PROC".equalsIgnoreCase(r.getCMD()) && r.getCallArr().size() != 1)
            return "SINGLE_PROC must have exactly 1 item";

        for (CallItem c : r.getCallArr()) {
            String e = validateSingle(c);
            if (e != null) return e;
        }
        return null;
    }

    private String validateSingle(CallItem c){
        if (empty(c.getCallId()))   return "CALL_ID required";
        if (empty(c.getAni()))      return "ANI required";
        if (empty(c.getCustmType()))return "CUSTM_TYPE required";
        if (empty(c.getFilePath())) return "FILE_PATH required";
        if (empty(c.getStartTime()))return "START_TIME required";
        if (empty(c.getEndSec()))   return "END_SEC required";
        if (empty(c.getDuration())) return "DURATION required";
        if (empty(c.getReqTime()))  return "REQ_TIME required";
        return null;
    }
    private boolean empty(String s){ return s==null || s.isBlank(); }

    // ---- responses ----
    private ResponseEntity<Map<String,Object>> ok(int queued) {
        return ResponseEntity.ok(Map.of("status","SUCCESS","queued",queued));
    }
    private ResponseEntity<Map<String,Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("status","FAIL","errorMsg",msg));
    }
    private ResponseEntity<Map<String,Object>> tooMany(String msg) {
        return ResponseEntity.status(429).body(Map.of("status","FAIL","errorMsg",msg));
    }
}
