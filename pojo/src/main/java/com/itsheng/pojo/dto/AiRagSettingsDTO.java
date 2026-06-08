package com.itsheng.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI/RAG 个性化偏好设置")
public class AiRagSettingsDTO {

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
}
