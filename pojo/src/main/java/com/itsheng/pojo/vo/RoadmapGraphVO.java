package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 职业地图图谱 VO
 */
@Data
public class RoadmapGraphVO {

    /**
     * 模式：vertical/lateral
     */
    private String mode;

    /**
     * 视图类型：global(全局)/focused(聚焦)
     */
    private String viewType;

    /**
     * 中心类别编码（聚焦视图时有效）
     */
    private String centerCategoryCode;

    /**
     * 节点列表
     */
    private List<RoadmapNodeVO> nodes;

    /**
     * 路径列表
     */
    private List<RoadmapPathVO> paths;
}
