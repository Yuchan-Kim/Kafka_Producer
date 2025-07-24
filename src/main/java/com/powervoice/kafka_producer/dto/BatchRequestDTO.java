package com.powervoice.kafka_producer.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchRequestDTO {
    private String CMD;
    private int TOTAL;
    private List<CallDataDTO> CALL_ARR;
    private String REQ_TIME;
}

