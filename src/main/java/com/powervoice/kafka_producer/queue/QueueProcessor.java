package com.powervoice.kafka_producer.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.dto.CallDataDTO;
import com.powervoice.kafka_producer.util.FileDownloader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class QueueProcessor {

    private final MessageQ messageQ;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileDownloader fileDownloader; // 추가

    private ExecutorService executorService;

    @PostConstruct
    public void start() {
        executorService = new ThreadPoolExecutor(
                0, 50,
                10L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        new Thread(() -> {
            while (true) {
                try {
                    CallDataDTO data = messageQ.take();
                    executorService.submit(() -> {
                        try {
                            byte[] audio = fileDownloader.download(data.getFilePath());
                            data.setAudio(audio); // 오디오 데이터를 CallDataDTO에 설정
                            String json = objectMapper.writeValueAsString(data);
                            kafkaTemplate.send("reqdata", json);
                            log.info("[Thread: executorService]: Sent to Kafka: {}", json);
                        } catch (Exception e) {
                            log.error("[Thread: executorService]: Kafka send error", e);
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "QueueDispatchThread").start();
    }
}
