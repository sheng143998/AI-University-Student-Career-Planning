package com.itsheng.service.service;

import com.itsheng.pojo.entity.JobEntity;
import java.util.List;

/**
 * 简历 - 岗位匹配 Service 接口（预留）
 */
public interface JobMatchingService {

    /**
     * 为指定用户的简历匹配推荐岗位
     * @param userId 用户 ID
     * @param limit 返回数量
     * @return 匹配的岗位列表
     */
    List<JobEntity> matchJobsForResume(Long userId, Integer limit);

    /**
     * 计算简历与岗位的匹配度
     * @param userId 用户 ID
     * @param jobId 岗位 ID
     * @return 匹配度分数（0-100）
     */
    Double calculateMatchScore(Long userId, Long jobId);
}
