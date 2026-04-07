package com.itsheng.service.service;

import com.itsheng.pojo.vo.CareerPathRecommendationVO;
import com.itsheng.pojo.vo.RoadmapGraphVO;
import com.itsheng.pojo.vo.RoadmapNodeDetailVO;
import com.itsheng.pojo.vo.RoadmapSearchResultVO;

/**
 * 职业地图服务接口
 */
public interface RoadmapService {

    /**
     * 搜索职业节点
     * @param keyword 关键字
     * @param limit 限制数量
     * @return 搜索结果
     */
    RoadmapSearchResultVO searchNodes(String keyword, Integer limit);

    /**
     * 获取地图图谱
     * @param categoryCode 岗位类别编码
     * @param mode 模式：vertical/lateral
     * @return 图谱数据
     */
    RoadmapGraphVO getGraph(String categoryCode, String mode);

    /**
     * 获取节点详情
     * @param id 节点ID
     * @return 节点详情
     */
    RoadmapNodeDetailVO getNodeDetail(Long id);

    /**
     * 获取个性化职业路径推荐
     * @return 个性化推荐结果（垂直晋升路径 + 横向换岗路径）
     */
    CareerPathRecommendationVO getPersonalizedRecommendations();
}
