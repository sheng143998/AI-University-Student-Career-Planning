package com.itsheng.pojo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobTransitionPath {
    private Long id;
    private Long fromJobId;
    private String fromJobName;
    private Long toJobId;
    private String toJobName;
    private Integer transitionDifficulty;
    private Integer avgTransitionTimeMonths;
    private String requiredSkillsGap;
    private BigDecimal similarityScore;
    private LocalDateTime createTime;
}
