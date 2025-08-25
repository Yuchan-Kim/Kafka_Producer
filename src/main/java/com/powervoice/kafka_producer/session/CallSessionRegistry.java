package com.powervoice.kafka_producer.session;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class CallSessionRegistry {

    // callId → "192.168.10.183:9382" 형태 저장 (매핑)
    private final ConcurrentHashMap<String, String> sessionMap = new ConcurrentHashMap<>();

    //callId에 대해 클라이언트 주소(IP:PORT)를 등록
    public void register(String callId, String ipWithPort) {
        sessionMap.put(callId, ipWithPort);  // 예: 192.168.10.183:9382
    }

    //등록된 클라이언트 주소(IP:PORT)를 반환
    public String getClientIp(String callId) {
        return sessionMap.get(callId);
    }

    //세션 등록 해제
    public void unregister(String callId) {
        sessionMap.remove(callId);
    }
}
