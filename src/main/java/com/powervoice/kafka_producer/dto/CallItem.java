package com.powervoice.kafka_producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CallItem {
    @JsonProperty("CALL_ID")     private String callId;       // REQUIRED
    @JsonProperty("ANI")         private String ani;          // REQUIRED
    @JsonProperty("CUSTM_TYPE")  private String custmType;    // REQUIRED
    @JsonProperty("CALL_TYPE")   private Integer callType;    // OPTIONAL
    @JsonProperty("EXT")         private String ext;          // OPTIONAL
    @JsonProperty("FILE_PATH")   private String filePath;     // REQUIRED
    @JsonProperty("START_TIME")  private String startTime;    // REQUIRED
    @JsonProperty("END_TIME")     private String endTime;       // REQUIRED
    @JsonProperty("DURATION")    private String duration;     // REQUIRED
    @JsonProperty("REQ_TIME")    private String reqTime;      // REQUIRED
}
