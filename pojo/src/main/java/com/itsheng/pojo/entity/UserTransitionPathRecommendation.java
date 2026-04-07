package com.itsheng.pojo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserTransitionPathRecommendation {
    private Long id;
    private Long userId;
    private String currentJobName;
    private Long matchedJobId;
    private Long recommendedPathId;
    private Long toJobId;
    private String toJobName;
    private Integer transitionDifficulty;
    private String requiredSkillsGap;
    private BigDecimal matchScore;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
