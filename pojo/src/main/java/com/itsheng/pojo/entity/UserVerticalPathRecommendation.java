package com.itsheng.pojo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UserVerticalPathRecommendation {
    private Long id;
    private Long userId;
    private String currentJobName;
    private Long matchedJobId;
    private String matchedJobName;
    private Long recommendedPathId;
    private String pathSteps;
    private Integer currentStepIndex;
    private Integer totalSteps;
    private Integer estimatedTotalMonths;
    private BigDecimal matchScore;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
