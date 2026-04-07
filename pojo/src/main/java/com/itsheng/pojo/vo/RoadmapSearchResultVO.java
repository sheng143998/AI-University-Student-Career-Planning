package com.itsheng.pojo.vo;

import lombok.Data;

import java.util.List;

/**
 * 职业地图搜索结果 VO
 */
@Data
public class RoadmapSearchResultVO {

    /**
     * 搜索结果项列表
     */
    private List<RoadmapSearchItemVO> items;
}
