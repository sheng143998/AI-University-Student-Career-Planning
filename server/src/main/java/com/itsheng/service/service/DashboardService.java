package com.itsheng.service.service;

import java.util.List;
import java.util.Map;

/**
 * Dashboard 服务接口
 */
public interface DashboardService {

    /**
     * 获取仪表盘汇总信息
     * @param userId 用户 ID
     * @return 仪表盘数据（job_profile, match_summary, market_trends, skill_radar, actions）
     */
    Map<String, Object> getSummary(Long userId);

    /**
     * 获取用户职业发展路径（roadmap）
     * 如果不存在则自动基于目标岗位创建
     * @param userId 用户 ID
     * @return roadmap 数据
     */
    Map<String, Object> getRoadmap(Long userId);

    /**
     * 更新用户当前所在阶段
     * @param userId 用户 ID
     * @param currentStepIndex 新的当前阶段索引
     * @return 更新后的 steps 列表
     */
    List<Map<String, Object>> updateCurrentStep(Long userId, Integer currentStepIndex);
}
