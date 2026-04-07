package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生就业能力画像实体
 * 对应数据库表：student_capability_profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentCapabilityProfile {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联 resume_analysis_result.id
     */
    private Long resumeAnalysisId;

    /**
     * 综合能力评分 (0-100)
     */
    private Integer overallScore;

    /**
     * 简历完整度评分 (0-100)
     */
    private Integer completenessScore;

    /**
     * 竞争力评分 (0-100)
     */
    private Integer competitivenessScore;

    /**
     * 各维度能力得分 (JSON)
     */
    private String capabilityScores;

    /**
     * 专业技能列表 (JSON)
     */
    private String professionalSkills;

    /**
     * 证书列表 (JSON)
     */
    private String certificates;

    /**
     * 软技能评估 (JSON)
     */
    private String softSkills;

    /**
     * AI 综合评价
     */
    private String aiEvaluation;

    /**
     * 生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
