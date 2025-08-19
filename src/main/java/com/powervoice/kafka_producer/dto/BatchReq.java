package com.powervoice.kafka_producer.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;


@Data
public class BatchReq {
    @JsonProperty("CMD")
    private String CMD;


    @JsonProperty("TOTAL")
    private Integer TOTAL;


    @JsonProperty("CALL_ARR")
    private List<CallData> callArr;
}