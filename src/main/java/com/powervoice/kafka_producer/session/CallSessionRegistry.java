package com.powervoice.kafka_producer.session;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallSessionRegistry {

    private final ConcurrentHashMap<String, String> sessionMap = new ConcurrentHashMap<>();

    public void register(String callId, String clientIp) {
        sessionMap.put(callId, clientIp);
    }

    public String getClientIp(String callId) {
        return sessionMap.get(callId);
    }

    public void unregister(String callId) {
        sessionMap.remove(callId);
    }
}

