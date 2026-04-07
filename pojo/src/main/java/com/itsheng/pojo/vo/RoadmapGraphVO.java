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
     * 节点列表
     */
    private List<RoadmapNodeVO> nodes;

    /**
     * 路径列表
     */
    private List<RoadmapPathVO> paths;
}
