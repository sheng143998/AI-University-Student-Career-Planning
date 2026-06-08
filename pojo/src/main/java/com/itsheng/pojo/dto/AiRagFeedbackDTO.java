package com.itsheng.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI/RAG 结果质量反馈请求")
public class AiRagFeedbackDTO {

    @JsonProperty("target_type")
    private String targetType;

    @JsonProperty("target_id")
    private String targetId;

    private Integer rating;

    @JsonProperty("reason_tags")
    private List<String> reasonTags;

    private String comment;

    @JsonProperty("retrieval_trace_id")
    private String retrievalTraceId;

    @JsonProperty("evidence_ref_ids")
    private List<String> evidenceRefIds;

    private String page;

    @JsonProperty("user_action")
    private String userAction;
}
