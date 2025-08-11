package com.powervoice.kafka_producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BatchReq {
    private String CMD;                 // REQUIRED: BATCH_PROC | SINGLE_PROC
    private Integer TOTAL;              // REQUIRED: == CALL_ARR.size()
    @JsonProperty("CALL_ARR")
    private List<CallItem> callArr;     // REQUIRED (단일도 길이 1)
}
