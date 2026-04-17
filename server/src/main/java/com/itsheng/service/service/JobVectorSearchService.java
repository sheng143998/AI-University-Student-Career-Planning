package com.itsheng.service.service;

import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.JobEntity;
import java.util.List;

/**
 * 岗位向量检索 Service 接口
 */
public interface JobVectorSearchService {

    /**
     * 根据查询文本搜索相似岗位
     * @param queryText 查询文本，可来自简历摘要、技能关键词、求职意向等
     * @param limit 返回数量
     * @return 相似岗位分类列表（JobCategory）
     */
    List<JobCategory> searchSimilarJobs(String queryText, Integer limit);

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
