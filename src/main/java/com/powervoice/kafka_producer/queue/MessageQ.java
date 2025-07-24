package com.powervoice.kafka_producer.queue;

import com.powervoice.kafka_producer.dto.CallDataDTO;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class MessageQ {
    private final BlockingQueue<CallDataDTO> queue = new LinkedBlockingQueue<>();

    public void add(CallDataDTO data) {
        queue.add(data);
    }

    public CallDataDTO take() throws InterruptedException {
        return queue.take();
    }
}
