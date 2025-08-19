//package com.powervoice.kafka_producer.controller;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.powervoice.kafka_producer.dto.BatchReq;
//import com.powervoice.kafka_producer.dto.CallData;
//import com.powervoice.kafka_producer.queue.MessageQ;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//
//import java.util.List;
//import java.util.Map;
//
//
//@RestController
//@RequestMapping("/reqdata")
//@RequiredArgsConstructor
//@Slf4j
//public class KafkaController {
//
//
//    private final ObjectMapper om;
//    private final MessageQ messageQ;
//
//
//    @Value("${app.queue.offer-timeout-ms:200}")
//    private long offerTimeoutMs;
//
//
//    @PostMapping
//    public ResponseEntity<Map<String, Object>> process(@RequestBody JsonNode node) {
//        try {
//            BatchReq req = om.treeToValue(node, BatchReq.class);
//            String err = validate(req);
//            if (err != null) return bad(err);
//
//
//            int enq = 0;
//            for (CallData item : req.getCallArr()) {
//                boolean ok = messageQ.offer(item, offerTimeoutMs);
//                if (!ok) return tooMany("QUEUE_FULL");
//                enq++;
//            }
//
//
//            log.info("Enqueued {} items (queue size={}, remain={})",
//                    enq, messageQ.size(), messageQ.remainingCapacity());
//            return ok(enq);
//
//
//        } catch (Exception e) {
//            log.error("Invalid request", e);
//            return bad("Invalid request: " + e.getMessage());
//        }
//    }
//
//
//    private String validate(BatchReq r) {
//        if (r.getCMD() == null || r.getCMD().isBlank())
//            return "CMD required";
//        if (!List.of("BATCH_PROC", "SINGLE_PROC").contains(r.getCMD()))
//            return "CMD must be BATCH_PROC or SINGLE_PROC";
//        if (r.getTOTAL() == null || r.getTOTAL() < 1)
//            return "TOTAL required (>0)";
//        if (r.getCallArr() == null || r.getCallArr().isEmpty())
//            return "CALL_ARR required";
//        if (!r.getTOTAL().equals(r.getCallArr().size()))
//            return "TOTAL mismatch";
//        if ("SINGLE_PROC".equalsIgnoreCase(r.getCMD()) && r.getCallArr().size() != 1)
//            return "SINGLE_PROC must have exactly 1 item";
//
//
//        for (CallData c : r.getCallArr()) {
//            String e = validateSingle(c);
//            if (e != null) return e;
//        }
//        return null;
//    }
//
//
//    private String validateSingle(CallData c) {
//        if (empty(c.getCallId())) return "callID required";
//        if (empty(c.getEmpNo())) return "empNo required";
//        if (empty(c.getPhoneNum())) return "phoneNum required";
//        if (empty(c.getFilePath())) return "filePath required";
//        if (empty(c.getStartTime())) return "startTime required";
//        if (empty(c.getEndTime())) return "endTime required";
//        if (empty(c.getDuration())) return "duration required";
//        return null;
//    }
//
//
//    private boolean empty(String s) {
//        return s == null || s.isBlank();
//    }
//
//
//    private ResponseEntity<Map<String, Object>> ok(int queued) {
//        return ResponseEntity.ok(Map.of("status", "SUCCESS", "queued", queued));
//    }
//
//
//    private ResponseEntity<Map<String, Object>> bad(String msg) {
//        return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "errorMsg", msg));
//    }
//
//
//    private ResponseEntity<Map<String, Object>> tooMany(String msg) {
//        return ResponseEntity.status(429).body(Map.of("status", "FAIL", "errorMsg", msg));
//    }
//}


package com.powervoice.kafka_producer.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.CallData;
import com.powervoice.kafka_producer.queue.MessageQ;
import com.powervoice.kafka_producer.session.CallSessionRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;


@RestController
@RequestMapping("/reqdata")
@RequiredArgsConstructor
@Slf4j
public class KafkaController {


    private final ObjectMapper om;
    private final MessageQ messageQ;
    private final CallSessionRegistry registry; // ← 세션 저장소 주입


    @Value("${app.queue.offer-timeout-ms:200}")
    private long offerTimeoutMs;

    @PostMapping
    public ResponseEntity<Map<String, Object>> process(@RequestBody CallData data,
                                                       HttpServletRequest request) {
        try {
            String clientIp = request.getRemoteAddr(); // ← 클라이언트 IP 추출
            registry.register(data.getCallId(), clientIp); // ← callId와 함께 저장

            boolean ok = messageQ.offer(data, offerTimeoutMs);
            if (!ok) return tooMany("QUEUE_FULL");

            log.info("✅ 등록: callId={}, empNo={}, ip={}, queueSize={}, remain={}",
                    data.getCallId(), data.getEmpNo(), clientIp, messageQ.size(), messageQ.remainingCapacity());

            return ResponseEntity.ok(Map.of("status", "SUCCESS"));

        } catch (Exception e) {
            log.error("❌ Invalid request", e);
            return bad("Invalid request: " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "errorMsg", msg));
    }

    private ResponseEntity<Map<String, Object>> tooMany(String msg) {
        return ResponseEntity.status(429).body(Map.of("status", "FAIL", "errorMsg", msg));
    }
}