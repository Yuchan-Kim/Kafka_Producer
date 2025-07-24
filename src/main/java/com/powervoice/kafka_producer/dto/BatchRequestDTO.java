// src/main/java/com/powervoice/kafka_producer/dto/BatchRequestDTO.java
package com.powervoice.kafka_producer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class BatchRequestDTO {
    @JsonProperty("CMD")
    private String CMD;

    @JsonProperty("TOTAL")
    private int TOTAL;

    @JsonProperty("CALL_ARR")
    private List<CallDataDTO> CALL_ARR;

    @JsonProperty("REQ_TIME")
    private String REQ_TIME;
}
