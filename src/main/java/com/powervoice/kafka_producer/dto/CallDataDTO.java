// src/main/java/com/powervoice/kafka_producer/dto/CallDataDTO.java
package com.powervoice.kafka_producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CallDataDTO {
    @JsonProperty("CALL_ID")
    private String callId;

    @JsonProperty("ANI")
    private String ani;

    @JsonProperty("CALL_TYPE")
    private String callType;

    @JsonProperty("EXT")
    private int ext;

    @JsonProperty("FILE_PATH")
    private String filePath;

    @JsonProperty("START_TIME")
    private String startTime;

    @JsonProperty("END_SEC")
    private String endSec;

    @JsonProperty("DURATION")
    private String duration;

    @JsonProperty("WAVPATH")
    private String wavpath;

    @JsonProperty("AUDIO")
    private byte[] audio;
}
