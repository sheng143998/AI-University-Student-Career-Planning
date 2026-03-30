package com.itsheng.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GoalMilestone {
    private Long id;
    private Long goalId;
    private Long userId;
    private String title;
    private String milestoneDesc;
    private String status;
    private Integer progress;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
