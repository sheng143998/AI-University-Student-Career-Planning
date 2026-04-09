package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 根据岗位名称/ID返回的垂直晋升路径明细（对齐前端 website/src/api/roadmap.ts 的 JobVerticalPathDetail）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobVerticalPathDetailVO {

    private Long jobId;
    private String jobName;
    private String jobLevel;
    private String jobLevelName;
    private List<JobVerticalPathVO> paths;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobVerticalPathVO {
        private Long id;
        private String pathType;
        private String targetJobName;
        private Integer totalSteps;
        private Integer estimatedTotalMonths;
        private BigDecimal confidenceScore;
        private List<PathStepVO> pathSteps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PathStepVO {
        private Integer step;
        private String jobName;
        private String jobLevel;
        private String jobLevelName;
        private List<String> skills;
        private Integer avgTimeMonths;
        private Integer difficulty;
        private String salaryRange;
    }
}
