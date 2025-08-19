package com.powervoice.kafka_producer.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


@Data
public class CallData {
    @JsonProperty("callId") private String callId;
    @JsonProperty("empNo") private String empNo;
    @JsonProperty("phoneNum") private String phoneNum;
    @JsonProperty("customerNum") private String customerNum;
    @JsonProperty("rcvType") private String rcvType;
    @JsonProperty("direction") private String direction;
    @JsonProperty("startTime") private String startTime;
    @JsonProperty("endTime") private String endTime;
    @JsonProperty("filePath") private String filePath;
    @JsonProperty("duration") private String duration;
    @JsonProperty("mobId") private String mobId;
    @JsonProperty("mobHistSeq") private String mobHistSeq;
}