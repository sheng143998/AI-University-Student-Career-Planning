package com.itsheng.service.service.Impl;

import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.JobVectorStore;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.JobVectorStoreMapper;
import com.itsheng.service.service.JobVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 岗位向量检索 Service 实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobVectorSearchServiceImpl implements com.itsheng.service.service.JobVectorSearchService {

    private final JobVectorStoreMapper jobVectorStoreMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final OpenAiEmbeddingModel embeddingModel;

    @Override
    public List<JobCategory> searchSimilarJobs(String resumeVector, Integer limit) {
        try {
            // 使用 job_vector_store 的余弦相似度搜索
            List<JobVectorStore> similarJobs = jobVectorStoreMapper.searchByVector(resumeVector, limit != null ? limit : 5);

            if (similarJobs.isEmpty()) {
                log.warn("未找到相似的岗位向量");
                return Collections.emptyList();
            }

            // 通过 job_vector_store.job_id → job 表的 source_job_ids 查找对应 JobCategory
            List<JobCategory> result = similarJobs.stream()
                    .map(jobVec -> {
                        if (jobVec.getJobId() != null) {
                            JobCategory category = jobCategoryMapper.selectBySourceJobId(jobVec.getJobId());
                            if (category != null) {
                                log.info("找到相似岗位: {} (job_id={}, category_id={})",
                                        category.getJobCategoryName(), jobVec.getJobId(), category.getId());
                                return category;
                            }
                        }
                        return null;
                    })
                    .filter(item -> item != null)
                    .distinct()
                    .toList();

            log.info("向量搜索找到 {} 个相似岗位", result.size());
            return result;

        } catch (Exception e) {
            log.error("岗位向量搜索失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getJobVector(Long jobId) {
        JobVectorStore jobVectorStore = jobVectorStoreMapper.selectByJobId(jobId);
        if (jobVectorStore == null) {
            log.warn("未找到 job_id={} 的向量数据", jobId);
            return null;
        }
        return jobVectorStore.getEmbeddingVector();
    }

    @Override
    public List<com.itsheng.pojo.entity.JobEntity> searchByKeyword(String keyword, Integer limit) {
        return jobCategoryMapper.searchByKeyword(keyword, limit).stream()
                .map(category -> com.itsheng.pojo.entity.JobEntity.builder()
                        .id(category.getId())
                        .jobName(category.getJobCategoryName())
                        .jobCode(category.getJobCategoryCode())
                        .build())
                .toList();
    }
}
