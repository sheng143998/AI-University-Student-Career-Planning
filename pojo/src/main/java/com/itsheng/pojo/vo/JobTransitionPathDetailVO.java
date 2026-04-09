package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 根据岗位名称/ID返回的换岗路径明细（对齐前端 website/src/api/roadmap.ts 的 JobTransitionPathDetail）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTransitionPathDetailVO {

    private Long jobId;
    private String jobName;
    private List<JobTransitionPathVO> transitionPaths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobTransitionPathVO {
        private Long id;
        private Long toJobId;
        private String toJobName;
        private Integer transitionDifficulty;
        private Integer avgTransitionTimeMonths;
        private Double similarityScore;
        private List<SkillsGapVO> requiredSkillsGap;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillsGapVO {
        private String skill;
        private Double level;
        private String priority;
    }
}
