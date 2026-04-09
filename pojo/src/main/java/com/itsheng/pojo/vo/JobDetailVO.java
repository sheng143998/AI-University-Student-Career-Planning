package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 岗位详情（对齐前端 website/src/api/roadmap.ts 的 JobDetail）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailVO {

    private Long id;
    private String jobName;
    private String jobLevel;
    private String jobLevelName;
    private String description;
    private String industry;
    private String salaryRange;
    private List<String> requiredSkills;
    private List<SkillAdviceVO> advancementSkills;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillAdviceVO {
        private String name;
        private String priority;
        private String advice;
    }
}
