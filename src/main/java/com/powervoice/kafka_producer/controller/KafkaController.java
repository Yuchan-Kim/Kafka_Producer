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

    @Value("${app.debug.auto_rep}")
    private boolean autoRep;

    @PostMapping
    public ResponseEntity<Map<String, Object>> process(@RequestBody CallData data, HttpServletRequest request) {
        try {
            // 1. 필수값 검증
            if (data == null) {
                return bad("[KafkaController_process] 수신할 데이터가 없습니다.");
            }
            if (data.getCallId() == null || data.getCallId().isBlank()) {
                return bad("[KafkaController_process] callId가 없습니다.");
            }
            if (data.getEmpNo() == null || data.getEmpNo().isBlank()) {
                return bad("[KafkaController_process] empNo가 없습니다.");
            }
            if (data.getPhoneNum() == null || data.getPhoneNum().isBlank()) {
                return bad("[KafkaController_process] phoneNum이 없습니다.");
            }
            if (data.getFilePath() == null || data.getFilePath().isBlank()) {
                return bad("[KafkaController_process] filePath가 없습니다.");
            }
            if (data.getStartTime() == null || data.getStartTime().isBlank()) {
                return bad("[KafkaController_process] startTime이 없습니다.");
            }
            if (data.getEndTime() == null || data.getEndTime().isBlank()) {
                return bad("[KafkaController_process] endTime이 없습니다.");
            }
            if (data.getDuration() == null || data.getDuration().isBlank()) {
                return bad("[KafkaController_process] duration이 없습니다.");
            }
            if (data.getDuration().equals("0") || data.getDuration().equals("0.0")) {
                return bad("[KafkaController_process] duration이 0입니다.");
            }



            // 2. IP 등록
            //autoRep이 true인 경우에만 수신된 ip 등록
            if (autoRep) {
                String clientIp = request.getRemoteAddr();  // 클라이언트 IP

                // 요청이 도착한 전체 주소로부터 포트 추론 (ex: Host: 192.168.0.183:9382)
                String hostHeader = request.getHeader("Host");
                String clientPort = "80"; // 기본값

                if (hostHeader != null && hostHeader.contains(":")) {
                    clientPort = hostHeader.split(":")[1];
                }

                String clientAddr = clientIp + ":" + clientPort;

                // callId → clientAddr 매핑 등록
                registry.register(data.getCallId(), clientAddr);
                log.info("[KafkaController_process] 해당 callid: {}에 ip 등록 완료: {}", data.getCallId(), clientAddr);
            }



            // 3. 큐 등록
            boolean ok;
            try {
                ok = messageQ.offer(data, offerTimeoutMs);
                log.info("[KafkaController_process] : Queue에 등록 성공: callId={}, empNo={}, queueSize={}, remain={}",
                        data.getCallId(), data.getEmpNo(), messageQ.size(), messageQ.remainingCapacity());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt(); // 인터럽트 상태 복원
                return ResponseEntity.status(503).body(Map.of(
                        "status", "FAIL",
                        "errorMsg", "[KafkaController_process]: Queue 등록 중 인터럽트 발생"
                ));
            }

            // 큐가 가득 찬 경우
            if (!ok) {
                return tooMany("[KafkaController_process]: 큐가 가득참");
            }


            log.info("[KafkaController_process] 수신한 데이터: callId={}, empNo={}, startTime={}, endTime={}",
                    data.getCallId(), data.getEmpNo(), data.getStartTime(), data.getEndTime());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "callId", data.getCallId(),
                    "empNo", data.getEmpNo(),
                    "duration", data.getDuration(),
                    "startTime", data.getStartTime(),
                    "endTime", data.getEndTime()
            ));

        } catch (IllegalArgumentException e) {
            log.warn("[KafkaController_process] 잘못된 인자: {}", e.getMessage());
            return bad("Invalid argument: " + e.getMessage());

        } catch (Exception e) {
            log.error("[KafkaController_process] 처리 중 알 수 없는 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAIL",
                    "errorMsg", "Unexpected error: " + e.getMessage()
            ));
        }
    }

    // 응답 헬퍼
    private ResponseEntity<Map<String, Object>> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("status", "FAIL", "errorMsg", msg));
    }

    // 429 Too Many Requests
    private ResponseEntity<Map<String, Object>> tooMany(String msg) {
        return ResponseEntity.status(429).body(Map.of("status", "FAIL", "errorMsg", msg));
    }
}