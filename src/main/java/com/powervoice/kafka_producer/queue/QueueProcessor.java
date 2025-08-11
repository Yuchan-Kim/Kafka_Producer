package com.powervoice.kafka_producer.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.CallItem;
import com.powervoice.kafka_producer.kafka.AudioProducer;
import com.powervoice.kafka_producer.util.FileDownloader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessor {
    private final MessageQ messageQ;
    private final FileDownloader downloader;
    private final AudioProducer producer;

    @Value("${app.worker.threads:8}") private int threads;
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
                CallItem item = messageQ.take();            // 큐에서 1건
                byte[] audio = downloader.download(item.getFilePath());
                if (audio == null || audio.length == 0) {
                    log.warn("Skip (download fail/empty): CALL_ID={}, path={}", item.getCallId(), item.getFilePath());
                    continue;
                }
                producer.send(item, audio);                 // 비동기 카프카 전송
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Worker error", e);
            }
        }
    }

    @PreDestroy
    public void stop(){ if (pool != null) pool.shutdownNow(); }
}
