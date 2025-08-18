// src/main/java/com/powervoice/kafka_producer/dto/BatchReq.java
package com.powervoice.kafka_producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BatchReq {
    @JsonProperty("CMD")
    private String CMD;                 // REQUIRED

    @JsonProperty("TOTAL")
    private Integer TOTAL;              // REQUIRED: == CALL_ARR.size()

    @JsonProperty("CALL_ARR")
    private List<CallItem> callArr;     // REQUIRED (단일도 길이 1)
}
