package com.itsheng.pojo.vo;

import lombok.Data;

/**
 * 职业地图路径 VO
 */
@Data
public class RoadmapPathVO {

    /**
     * 起始节点 ID
     */
    private String from;

    /**
     * 目标节点 ID
     */
    private String to;

    /**
     * 变体：primary/secondary
     */
    private String variant;

    /**
     * 边类型：vertical/lateral
     */
    private String edgeType;

    /**
     * 线条样式：dashed/solid
     */
    private String lineStyle;

    /**
     * 转换难度（1-5）
     */
    private Integer difficulty;

    /**
     * 平均转换时间（月）
     */
    private Integer avgTimeMonths;

    /**
     * 成功率（0-1）
     */
    private Double successRate;
}
