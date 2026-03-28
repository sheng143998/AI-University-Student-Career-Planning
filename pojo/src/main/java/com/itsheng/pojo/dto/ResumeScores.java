package com.itsheng.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历评分 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeScores {
    /**
     * 关键词匹配度（0-100）
     */
    @JsonProperty("keyword_match")
    private Integer keywordMatch;

    /**
     * 布局排版评分（0-100）
     */
    @JsonProperty("layout")
    private Integer layout;

    /**
     * 技能深度评分（0-100）
     */
    @JsonProperty("skill_depth")
    private Integer skillDepth;

    /**
     * 经历评分（0-100）
     */
    @JsonProperty("experience")
    private Integer experience;
}
