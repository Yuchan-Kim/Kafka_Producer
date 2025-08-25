package com.powervoice.kafka_producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/*
 * 2025-08-22
 * @Author: 김유찬 프로
 * 1. Reqdata 토픽으로 /reqdata POST 요청을 통해 수신된 JSON 메시지를 전송
 * 2. debug 모드에서만 Class, Sttdia, Summary 토픽 메세지를 전부 전송
 * 3. auto_rep=true 인 경우, 수신된 IP를 통해서 전송, false인 경우 설정된 target_add로 전송
 */


@SpringBootApplication
public class KafkaProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApplication.class, args);
    }

}
