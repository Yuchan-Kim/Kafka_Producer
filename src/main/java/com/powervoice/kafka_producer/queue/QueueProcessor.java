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
        while (!Thread.currentThread().isInterrupted()) {
            try {
                CallData item = messageQ.take();
// 실제 처리 대신 콘솔 출력
                System.out.printf("[RECEIVED] callId=%s, empNo=%s, startTime=%s, endTime=%s\n",
                        item.getCallId(), item.getEmpNo(), item.getStartTime(), item.getEndTime());
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