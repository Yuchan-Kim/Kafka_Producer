package com.powervoice.kafka_producer.dto;
import lombok.Data;

@Data
public class CallDataDTO {
    private String callId;
    private String ani;
    private String callType;
    private int ext;
    private String filePath;
    private String startTime;
    private String endSec;
    private String duration;
}
