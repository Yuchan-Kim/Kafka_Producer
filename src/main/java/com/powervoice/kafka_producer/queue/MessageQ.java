//package com.powervoice.kafka_producer.queue;
//
//
//import com.powervoice.kafka_producer.dto.CallData;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//
//
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//import java.util.concurrent.TimeUnit;
//
//
//@Component
//public class MessageQ {
//    private final BlockingQueue<CallData> q;
//
//
//    public MessageQ(@Value("${app.queue.capacity:10000}") int cap) {
//        this.q = new LinkedBlockingQueue<>(cap);
//    }
//
//
//    public boolean offer(CallData x, long timeoutMs) throws InterruptedException {
//        return q.offer(x, timeoutMs, TimeUnit.MILLISECONDS);
//    }
//
//
//    public CallData take() throws InterruptedException {
//        return q.take();
//    }
//
//
//    public int size() {
//        return q.size();
//    }
//
//
//    public int remainingCapacity() {
//        return q.remainingCapacity();
//    }
//}

package com.powervoice.kafka_producer.queue;


import com.powervoice.kafka_producer.dto.CallData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


@Component
public class MessageQ {
    private final BlockingQueue<CallData> q;


    public MessageQ(@Value("${app.queue.capacity:10000}") int cap) {
        this.q = new LinkedBlockingQueue<>(cap);
        System.out.println("✅ MessageQ 생성됨 (capacity=" + cap + ")");
    }



    public boolean offer(CallData x, long timeoutMs) throws InterruptedException {
        return q.offer(x, timeoutMs, TimeUnit.MILLISECONDS);
    }


    public CallData take() throws InterruptedException {
        return q.take();
    }


    public int size() {
        return q.size();
    }


    public int remainingCapacity() {
        return q.remainingCapacity();
    }
}