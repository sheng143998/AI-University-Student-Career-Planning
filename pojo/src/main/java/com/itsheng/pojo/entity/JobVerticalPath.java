package com.itsheng.pojo.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class JobVerticalPath {
    private Long id;
    private Long jobId;
    private String jobName;
    private Long targetJobId;
    private String targetJobName;
    private String pathSteps;
    private Integer totalSteps;
    private Integer estimatedTotalMonths;
    private BigDecimal confidenceScore;
    private String pathType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
