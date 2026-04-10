package com.itsheng.service.service;

import com.itsheng.pojo.vo.CareerPathRecommendationVO;
import com.itsheng.pojo.vo.JobDetailVO;
import com.itsheng.pojo.vo.JobSearchResultVO;
import com.itsheng.pojo.vo.JobVerticalPathDetailVO;
import com.itsheng.pojo.vo.RoadmapGraphVO;
import com.itsheng.pojo.vo.RoadmapNodeDetailVO;
import com.itsheng.pojo.vo.RoadmapSearchResultVO;
import com.itsheng.pojo.vo.UserTransitionRecommendationVO;

import java.util.List;

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
     * 搜索岗位（对齐前端 /api/roadmap/jobs/search）
     */
    List<JobSearchResultVO> searchJobs(String q, Integer limit);

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
     * 根据岗位名称获取晋升路径（对齐前端 /api/roadmap/map/path-by-name）
     */
    JobVerticalPathDetailVO getVerticalPathByJobName(String jobName);

    /**
     * 根据岗位名称获取换岗推荐（对齐前端 /api/roadmap/recommend/transition/by-job）
     */
    UserTransitionRecommendationVO recommendTransitionByJobName(String jobName);

    /**
     * 获取岗位详情（对齐前端 /api/roadmap/map/job-detail/{id}）
     */
    JobDetailVO getJobDetail(Long id);

    /**
     * 获取个性化职业路径推荐
     * @return 个性化推荐结果（垂直晋升路径 + 横向换岗路径）
     */
    CareerPathRecommendationVO getPersonalizedRecommendations();

    /**
     * Clear personalized recommendations cache for current user
     */
    void clearPersonalizedRecommendationsCache();

    /**
     * 保存用户手动设置的当前岗位
     * @param currentJob 当前岗位
     */
    void saveUserCurrentJob(String currentJob);

    /**
     * 获取用户手动设置的当前岗位
     * @return 当前岗位，如果未设置则返回null
     */
    String getUserCurrentJob();

    /**
     * Get vertical path by job name and level
     */
    JobVerticalPathDetailVO getVerticalPathByJobNameAndLevel(String jobName, String level);

    /**
     * Get transition recommendations by job name and level
     */
    UserTransitionRecommendationVO recommendTransitionByJobNameAndLevel(String jobName, String level);
}
