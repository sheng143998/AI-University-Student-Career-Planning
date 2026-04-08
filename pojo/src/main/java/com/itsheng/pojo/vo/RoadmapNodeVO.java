package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 职业地图节点 VO
 */
@Data
public class RoadmapNodeVO {

    /**
     * 节点 ID（岗位 ID）
     */
    private String id;

    /**
     * 岗位标题
     */
    private String title;

    /**
     * 显示标签
     */
    private String label;

    /**
     * 副标题（年限等）
     */
    private String subtitle;

    /**
     * 显示副标签
     */
    private String subLabel;

    /**
     * 种类：core/secondary
     */
    private String kind;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * X 坐标
     */
    private Integer x;

    /**
     * Y 坐标
     */
    private Integer y;

    /**
     * 变体：primary/neutral
     */
    private String variant;

    /**
     * 类别编码
     */
    private String categoryCode;

    /**
     * 岗位级别
     */
    private String level;
}
