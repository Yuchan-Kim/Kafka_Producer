package com.powervoice.kafka_producer.controller;

import com.powervoice.kafka_producer.kafka.MultiTopicProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class KafkaTestController {

    private final MultiTopicProducer producer;

    @PostMapping("/send/call")
    public String sendCall(@RequestBody String message) {
        producer.sendToCallTopic(message);
        return "Sent to topic-call";
    }

    @PostMapping("/send/mask")
    public String sendMask(@RequestBody String message) {
        producer.sendToMaskTopic(message);
        return "Sent to topic-mask";
    }

    @PostMapping("/send/result")
    public String sendResult(@RequestBody String message) {
        producer.sendToResultTopic(message);
        return "Sent to topic-result";
    }

    @PostMapping("/send/notify")
    public String sendNotify(@RequestBody String message) {
        producer.sendToNotifyTopic(message);
        return "Sent to topic-notify";
    }
}
