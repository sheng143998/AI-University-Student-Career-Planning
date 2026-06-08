package com.itsheng.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AI/RAG 个性化偏好设置结果")
public class AiRagSettingsVO {

    @JsonProperty("enable_ai_advice_notifications")
    private Boolean enableAiAdviceNotifications;

    @JsonProperty("enable_rag_personalization")
    private Boolean enableRagPersonalization;

    @JsonProperty("preferred_city")
    private String preferredCity;

    @JsonProperty("preferred_industries")
    private List<String> preferredIndustries;

    @JsonProperty("preferred_job_levels")
    private List<String> preferredJobLevels;

    @JsonProperty("career_direction")
    private String careerDirection;

    @JsonProperty("result_language")
    private String resultLanguage;

    @JsonProperty("feedback_usage_scope")
    private String feedbackUsageScope;

    private Boolean updated;

    @JsonProperty("effective_filters")
    private Map<String, Object> effectiveFilters;

    private Map<String, Object> diagnostics;
}
