//package com.powervoice.kafka_producer.queue;
//
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.powervoice.kafka_producer.dto.CallData;
//import com.powervoice.kafka_producer.kafka.JsonProducer;
//import jakarta.annotation.PostConstruct;
//import jakarta.annotation.PreDestroy;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//
//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class QueueProcessor {
//    private final MessageQ messageQ;
//    private final JsonProducer producer;
//    private final ObjectMapper objectMapper;
//
//
//    @Value("${app.worker.threads:8}")
//    private int threads;
//
//
//    private ExecutorService pool;
//
//
//    @PostConstruct
//    public void start() {
//        pool = Executors.newFixedThreadPool(threads);
//        for (int i = 0; i < threads; i++) pool.submit(this::runLoop);
//        log.info("QueueProcessor started with {} threads", threads);
//    }
//
//
//    private void runLoop() {
//        while (!Thread.currentThread().isInterrupted()) {
//            try {
//                CallData item = messageQ.take();
//                String rawJson = objectMapper.writeValueAsString(item);
//                producer.sendSingle(item.getCallId(), rawJson);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            } catch (Exception e) {
//                log.error("Worker error", e);
//            }
//        }
//    }
//
//
//    @PreDestroy
//    public void stop() {
//        if (pool != null) pool.shutdownNow();
//    }
//}


package com.powervoice.kafka_producer.queue;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.CallData;
import com.powervoice.kafka_producer.kafka.JsonProducer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessor {
    private final MessageQ messageQ;
    private final JsonProducer jsonProducer;  // ✅ 추가

    @Value("${app.worker.threads:8}")
    private int threads;

    private ExecutorService pool;

    @PostConstruct
    public void start() {
        pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) pool.submit(this::runLoop);
        log.info("QueueProcessor started with {} threads", threads);
    }

    private void runLoop() {
        log.info("▶️ runLoop 진입 - Thread={}", Thread.currentThread().getName());
        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.info("🟡 waiting on messageQ.take()");
                CallData item = messageQ.take();
                log.info("🟢 TAKE 완료 - callId={}", item.getCallId());

                String key = item.getCallId();
                String json = new ObjectMapper().writeValueAsString(item);  // 또는 별도 유틸

                log.info("📤 Kafka 전송 시작: callId={}", key);
                jsonProducer.sendSingle(key, json);  // ✅ 전송
                log.info("📬 Kafka 전송 완료: callId={}", key);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (pool != null) pool.shutdownNow();
    }
}
