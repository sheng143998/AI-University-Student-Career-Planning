package com.itsheng.pojo.dto;

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
    private Integer keywordMatch;

    /**
     * 布局排版评分（0-100）
     */
    private Integer layout;

    /**
     * 技能深度评分（0-100）
     */
    private Integer skillDepth;

    /**
     * 经历评分（0-100）
     */
    private Integer experience;
}
