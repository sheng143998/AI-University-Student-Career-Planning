package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户职业发展路径实体
 * 对应数据库表：user_roadmap_steps
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoadmapSteps {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 关联岗位 ID
     */
    private Long jobProfileId;

    /**
     * 当前所在阶段索引
     */
    private Integer currentStepIndex;

    /**
     * 职业发展阶段列表（JSON）
     */
    private String steps;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
