package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户职业数据实体
 * 对应数据库表：user_career_data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCareerData {

    /**
     * 主键
     */
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户目标岗位名称
     */
    private String targetJob;

    /**
     * 用户目标岗位ID（关联 job 表）
     */
    private Long targetJobId;

    /**
     * 岗位画像信息（JSON）
     */
    private String jobProfile;

    /**
     * 匹配度摘要（JSON）
     */
    private String matchSummary;

    /**
     * 市场趋势数据（JSON）
     */
    private String marketTrends;

    /**
     * 能力雷达图数据（JSON）
     */
    private String skillRadar;

    /**
     * 行动建议列表（JSON）
     */
    private String actions;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
