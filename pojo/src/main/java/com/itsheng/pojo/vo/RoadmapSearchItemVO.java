package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 职业地图搜索结果项 VO
 */
@Data
public class RoadmapSearchItemVO {

    /**
     * 节点 ID
     */
    private String id;

    /**
     * 基础类别编码（去掉级别后缀）
     */
    private String categoryCode;

    /**
     * 标题
     */
    private String title;

    /**
     * 副标题
     */
    private String subtitle;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 变体：primary/neutral
     */
    private String variant;

    /**
     * 是否有垂直晋升路径
     */
    private Boolean hasVerticalPaths;

    /**
     * 是否有横向换岗路径
     */
    private Boolean hasLateralPaths;
}
