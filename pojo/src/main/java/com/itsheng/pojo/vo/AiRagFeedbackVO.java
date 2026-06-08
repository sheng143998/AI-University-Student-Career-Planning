package com.itsheng.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "AI/RAG 反馈接收结果")
public class AiRagFeedbackVO {

    @JsonProperty("feedback_id")
    private String feedbackId;

    private Boolean accepted;

    @JsonProperty("used_for")
    private List<String> usedFor;

    @JsonProperty("quality_dimensions")
    private Map<String, Object> qualityDimensions;

    private Map<String, Object> diagnostics;
}
