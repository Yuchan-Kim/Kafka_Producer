// src/test/java/com/powervoice/kafka_producer/controller/KafkaControllerTest.java
package com.powervoice.kafka_producer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powervoice.kafka_producer.controller.KafkaController;
import com.powervoice.kafka_producer.queue.MessageQ;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
// 반드시 이 경로로 import
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KafkaController.class)
class KafkaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageQ messageQ;  // MessageQ를 테스트 컨텍스트에 모킹 빈으로 등록

    // 배치용 JSON 생성 헬퍼
    private String buildBatchJson(int total) throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("CMD", "BATCH_PROC");
        node.put("TOTAL", total);
        var arr = node.putArray("CALL_ARR");
        for (int i = 0; i < total; i++) {
            var c = objectMapper.createObjectNode();
            c.put("CALL_ID",    "id" + i);
            c.put("ANI",        "ani" + i);
            c.put("CALL_TYPE",  "INBOUND");
            c.put("EXT",        1000 + i);
            c.put("FILE_PATH",  "/file" + i + ".wav");
            c.put("START_TIME","2025-07-24 09:00:00");
            c.put("END_SEC",    "15000");
            c.put("DURATION",   "15.0");
            arr.add(c);
        }
        node.put("REQ_TIME", "2025-07-24T12:00:00+09:00");
        return objectMapper.writeValueAsString(node);
    }

    // 단일 건용 JSON 생성 헬퍼
    private String buildSingleJson() throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("CALL_ID",   "idX");
        node.put("ANI",       "aniX");
        node.put("CALL_TYPE", "OUTBOUND");
        node.put("EXT",       1234);
        node.put("FILE_PATH","/fileX.wav");
        node.put("START_TIME","2025-07-24 10:00:00");
        node.put("END_SEC",   "16000");
        node.put("DURATION",  "16.0");
        return objectMapper.writeValueAsString(node);
    }

    @Test
    @DisplayName("배치 요청 성공")
    void batchSuccess() throws Exception {
        String json = buildBatchJson(3);

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.queued").value(3));

        verify(messageQ, times(3)).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("배치 TOTAL 불일치 오류")
    void batchTotalMismatch() throws Exception {
        String json = buildBatchJson(3).replaceFirst("\"TOTAL\":3", "\"TOTAL\":2");

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errorMsg").value("TOTAL and CALL_ARR size mismatch"));
    }

    @Test
    @DisplayName("단일 요청 성공")
    void singleSuccess() throws Exception {
        String json = buildSingleJson();

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.queued").value(1));

        verify(messageQ, times(1)).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("단일 요청 필수 필드 누락 오류")
    void singleMissingField() throws Exception {
        String json = buildSingleJson().replaceFirst("\"CALL_ID\":\"idX\",", "");

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"))
                .andExpect(jsonPath("$.errorMsg").value("CALL_ID is required"));
    }

    @Test
    @DisplayName("잘못된 JSON 포맷 오류")
    void invalidJson() throws Exception {
        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not: valid json "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단일 요청 성공 - WAVPATH 포함")
    void singleWithWavpath() throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("CALL_ID",   "idY");
        node.put("ANI",       "aniY");
        node.put("CALL_TYPE", "OUTBOUND");
        node.put("EXT",       1235);
        node.put("FILE_PATH", "/fileY.wav");
        node.put("START_TIME", "2025-07-24 10:10:00");
        node.put("END_SEC",   "17000");
        node.put("DURATION",  "17.0");
        node.put("WAVPATH",   "/mnt/recordings/2025/07/24/call_audio.wav");

        String json = objectMapper.writeValueAsString(node);

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.queued").value(1));

        verify(messageQ, times(1)).add(org.mockito.ArgumentMatchers.any());
    }


    @Test
    @DisplayName("배치 요청 성공 - WAVPATH 포함")
    void batchWithWavpath() throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("CMD", "BATCH_PROC");
        node.put("TOTAL", 2);
        var arr = node.putArray("CALL_ARR");

        for (int i = 0; i < 2; i++) {
            var c = objectMapper.createObjectNode();
            c.put("CALL_ID",    "id" + i);
            c.put("ANI",        "ani" + i);
            c.put("CALL_TYPE",  "INBOUND");
            c.put("EXT",        1000 + i);
            c.put("FILE_PATH",  "/file" + i + ".wav");
            c.put("START_TIME", "2025-07-24 09:00:00");
            c.put("END_SEC",    "15000");
            c.put("DURATION",   "15.0");
            c.put("WAVPATH",    "/mnt/recordings/2025/07/24/call_audio_" + i + ".wav");
            arr.add(c);
        }

        node.put("REQ_TIME", "2025-07-24T12:00:00+09:00");

        String json = objectMapper.writeValueAsString(node);

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.queued").value(2));

        verify(messageQ, times(2)).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("배치 요청 실패 - CALL_ARR 비어있음")
    void batchWithEmptyCallArr() throws Exception {
        var node = objectMapper.createObjectNode();
        node.put("CMD", "BATCH_PROC");
        node.put("TOTAL", 0);
        node.putArray("CALL_ARR"); // 비어 있음
        node.put("REQ_TIME", "2025-07-24T12:00:00+09:00");

        String json = objectMapper.writeValueAsString(node);

        mockMvc.perform(post("/reqdata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAIL"));
    }


}
