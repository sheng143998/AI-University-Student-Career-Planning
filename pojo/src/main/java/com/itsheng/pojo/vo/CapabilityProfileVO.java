package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 学生能力画像 VO
 * 对应接口：GET /api/resume/capability-profile
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapabilityProfileVO {
    /**
     * 画像记录 ID
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

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
     * 各维度能力得分
     */
    private Map<String, Integer> capabilityScores;

    /**
     * 专业技能列表
     */
    private List<ProfessionalSkill> professionalSkills;

    /**
     * 证书列表
     */
    private List<String> certificates;

    /**
     * 软技能评估详情
     */
    private Map<String, SoftSkillDetail> softSkills;

    /**
     * AI 综合评价文字
     */
    private String aiEvaluation;

    /**
     * 生成时间
     */
    private String generatedAt;

    /**
     * 专业技能详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfessionalSkill {
        /**
         * 技能名称
         */
        private String name;

        /**
         * 熟练度 (1-5)
         */
        private Integer proficiency;

        /**
         * 使用年限
         */
        private Integer years;

        /**
         * 能力证明
         */
        private String evidence;
    }

    /**
     * 软技能评估详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoftSkillDetail {
        /**
         * 评分 (0-100)
         */
        private Integer score;

        /**
         * 证据列表
         */
        private List<String> evidence;

        /**
         * 一句话描述
         */
        private String description;
    }
}
