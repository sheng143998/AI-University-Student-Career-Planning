package com.itsheng.service.service;

import com.itsheng.pojo.entity.JobEntity;
import java.util.List;

/**
 * 岗位向量检索 Service 接口（预留）
 */
public interface JobVectorSearchService {

    /**
     * 根据简历向量搜索相似岗位
     * @param resumeVector 简历 embedding 向量（格式："[0.1,0.2,...]"）
     * @param limit 返回数量
     * @return 相似岗位列表
     */
    List<JobEntity> searchSimilarJobs(String resumeVector, Integer limit);

    /**
     * 根据岗位 ID 获取向量
     * @param jobId 岗位 ID
     * @return embedding 向量（格式："[0.1,0.2,...]"）
     */
    String getJobVector(Long jobId);

    /**
     * 根据关键词搜索岗位
     * @param keyword 关键词
     * @param limit 返回数量
     * @return 岗位列表
     */
    List<JobEntity> searchByKeyword(String keyword, Integer limit);
}
