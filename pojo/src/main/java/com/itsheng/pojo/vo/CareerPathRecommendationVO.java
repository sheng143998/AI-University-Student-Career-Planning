package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 个性化职业路径推荐 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerPathRecommendationVO {

    /**
     * 用户当前岗位
     */
    private String currentJob;

    /**
     * 垂直晋升路径推荐
     */
    private VerticalPathRecommendationVO verticalPath;

    /**
     * 横向换岗路径推荐（不少于2条）
     */
    private List<LateralPathRecommendationVO> lateralPaths;

    /**
     * 推荐生成时间
     */
    private String generatedAt;

    /**
     * 垂直晋升路径推荐详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VerticalPathRecommendationVO {
        /**
         * 匹配的基础类别编码
         */
        private String categoryCode;

        /**
         * 匹配的职业名称
         */
        private String matchedJobName;

        /**
         * 相似度分数 (0-1)
         */
        private BigDecimal similarityScore;

        /**
         * 晋升路径节点列表
         */
        private List<PathNodeVO> nodes;

        /**
         * 当前用户所在级别索引
         */
        private Integer currentLevelIndex;

        /**
         * 预计晋升到下一级所需月数
         */
        private Integer estimatedMonthsToNext;
    }

    /**
     * 横向换岗路径推荐详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LateralPathRecommendationVO {
        /**
         * 目标岗位ID
         */
        private Long targetJobId;

        /**
         * 目标岗位名称
         */
        private String targetJobName;

        /**
         * 目标岗位类别编码
         */
        private String targetCategoryCode;

        /**
         * 匹配分数 (0-1)
         */
        private BigDecimal matchScore;

        /**
         * 转型难度 (1-5)
         */
        private Integer transitionDifficulty;

        /**
         * 预计转型所需月数
         */
        private Integer estimatedMonths;

        /**
         * 所需补充技能列表
         */
        private List<String> requiredSkills;

        /**
         * 已具备技能列表
         */
        private List<String> possessedSkills;

        /**
         * AI推荐理由
         */
        private String aiRecommendationReason;

        /**
         * 转型路径节点
         */
        private List<PathNodeVO> pathNodes;
    }

    /**
     * 路径节点VO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathNodeVO {
        /**
         * 节点ID
         */
        private String id;

        /**
         * 岗位名称
         */
        private String title;

        /**
         * 级别名称
         */
        private String levelName;

        /**
         * 薪资范围
         */
        private String salaryRange;

        /**
         * 所需技能
         */
        private List<String> skills;

        /**
         * 是否为用户当前级别
         */
        private Boolean isCurrentLevel;
    }
}
