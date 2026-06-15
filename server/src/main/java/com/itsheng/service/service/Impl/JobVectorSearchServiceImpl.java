package com.itsheng.service.service.Impl;

import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.JobEntity;
import com.itsheng.pojo.entity.JobVectorStore;
import com.itsheng.service.client.PythonMarketAiClient;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.JobVectorStoreMapper;
import com.itsheng.service.service.JobVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 基于 Python RAG 服务的岗位语义检索实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobVectorSearchServiceImpl implements JobVectorSearchService {

    private final JobCategoryMapper jobCategoryMapper;
    private final JobVectorStoreMapper jobVectorStoreMapper;
    private final PythonMarketAiClient pythonMarketAiClient;

    @Override
    public List<JobCategory> searchSimilarJobs(String queryText, Integer limit) {
        if (queryText == null || queryText.isBlank()) {
            log.warn("岗位检索查询文本为空");
            return Collections.emptyList();
        }

        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        if (allJobs == null || allJobs.isEmpty()) {
            log.warn("job 表中没有可用岗位");
            return Collections.emptyList();
        }

        int topK = limit != null && limit > 0 ? limit : 5;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("request_id", "job-search-" + UUID.randomUUID());
            payload.put("query_text", queryText);
            payload.put("limit", topK);
            payload.put("jobs", allJobs.stream()
                    .filter(job -> job != null && job.getId() != null)
                    .map(this::buildCandidatePayload)
                    .toList());
            payload.put("filters", Map.of(
                    "document_type", List.of("job", "jd"),
                    "visibility_scope", "public"
            ));

            PythonMarketAiClient.JobSearchResult searchResult = pythonMarketAiClient.searchJobs(payload);
            Map<Long, JobCategory> byId = allJobs.stream()
                    .filter(job -> job.getId() != null)
                    .collect(Collectors.toMap(JobCategory::getId, Function.identity(), (left, right) -> left));
            List<JobCategory> result = searchResult.jobIds().stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .limit(topK)
                    .toList();

            if (!result.isEmpty()) {
                result.forEach(job -> log.info("Python 岗位检索命中 {} (jobId={})",
                        job.getJobCategoryName(), job.getId()));
                return result;
            }
            log.info("Python 岗位检索无结果，使用关键词检索兜底，queryText={}", queryText);
            return fallbackSearch(queryText, topK);
        } catch (PythonMarketAiClient.PythonMarketException e) {
            log.warn("Python 岗位检索不可用，使用关键词检索兜底: {}", e.getMessage());
            return fallbackSearch(queryText, topK);
        } catch (Exception e) {
            log.error("岗位语义搜索失败: {}", e.getMessage(), e);
            return fallbackSearch(queryText, topK);
        }
    }

    @Override
    public String getJobVector(Long jobId) {
        if (jobId == null) {
            return null;
        }
        JobVectorStore vectorStore = jobVectorStoreMapper.selectByJobId(jobId);
        if (vectorStore == null || vectorStore.getEmbeddingVector() == null || vectorStore.getEmbeddingVector().isBlank()) {
            log.warn("未找到岗位向量，jobId: {}", jobId);
            return null;
        }
        return vectorStore.getEmbeddingVector();
    }

    @Override
    public List<JobEntity> searchByKeyword(String keyword, Integer limit) {
        return jobCategoryMapper.searchByKeyword(keyword, limit).stream()
                .map(category -> JobEntity.builder()
                        .id(category.getId())
                        .jobName(category.getJobCategoryName())
                        .jobCode(category.getJobCategoryCode())
                        .build())
                .toList();
    }

    private Map<String, Object> buildCandidatePayload(JobCategory job) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("job_id", job.getId());
        payload.put("job_name", job.getJobCategoryName());
        payload.put("job_category_code", job.getJobCategoryCode());
        payload.put("job_level", job.getJobLevel());
        payload.put("required_skills", job.getRequiredSkills());
        payload.put("job_description", job.getJobDescription());
        payload.put("job_profile", job.getJobProfile());
        payload.put("source_job_count", job.getSourceJobCount());
        return payload;
    }

    private List<JobCategory> fallbackSearch(String queryText, int limit) {
        try {
            List<JobCategory> fallback = jobCategoryMapper.searchByKeyword(queryText, limit);
            return fallback == null ? Collections.emptyList() : fallback;
        } catch (Exception e) {
            log.warn("岗位关键词检索兜底失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
