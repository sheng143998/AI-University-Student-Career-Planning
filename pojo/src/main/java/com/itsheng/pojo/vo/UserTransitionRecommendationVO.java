package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 推荐用户换岗路径（对齐前端 website/src/api/roadmap.ts 的 UserTransitionRecommendation）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTransitionRecommendationVO {

    private List<String> currentSkills;
    private List<TransitionRecommendationItemVO> recommendations;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransitionRecommendationItemVO {
        private Long id;
        private Long recommendationId;
        private Long toJobId;
        private String toJobName;
        private String toJobLevel;
        private String toJobLevelName;
        private Double matchScore;
        private Integer transitionDifficulty;
        private Integer avgTransitionTimeMonths;
        private List<JobTransitionPathDetailVO.SkillsGapVO> requiredSkillsGap;
        private String industry;
        private String salaryRange;
    }
}
