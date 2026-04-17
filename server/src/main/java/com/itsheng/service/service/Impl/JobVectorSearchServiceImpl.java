package com.itsheng.service.service.Impl;

import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.service.JobVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 job.job_category_name 的岗位检索实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobVectorSearchServiceImpl implements JobVectorSearchService {

    private static final int EMBEDDING_BATCH_SIZE = 10;

    private final JobCategoryMapper jobCategoryMapper;
    private final OpenAiEmbeddingModel embeddingModel;

    private final Map<Long, CachedJobEmbedding> embeddingCache = new ConcurrentHashMap<>();

    @Override
    public List<JobCategory> searchSimilarJobs(String queryText, Integer limit) {
        try {
            if (queryText == null || queryText.isBlank()) {
                log.warn("岗位检索查询文本为空");
                return Collections.emptyList();
            }

            List<JobCategory> allJobs = jobCategoryMapper.selectAll();
            if (allJobs == null || allJobs.isEmpty()) {
                log.warn("job 表中没有可用岗位");
                return Collections.emptyList();
            }

            refreshJobEmbeddings(allJobs);

            float[] queryEmbedding = embeddingModel.embed(queryText);
            if (queryEmbedding == null || queryEmbedding.length == 0) {
                log.warn("查询文本向量化失败，queryText: {}", queryText);
                return Collections.emptyList();
            }

            int topK = limit != null && limit > 0 ? limit : 5;
            List<JobSimilarity> matches = allJobs.stream()
                    .map(job -> buildSimilarity(job, queryEmbedding))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingDouble(JobSimilarity::similarity).reversed())
                    .limit(topK)
                    .toList();

            List<JobCategory> result = matches.stream()
                    .map(JobSimilarity::job)
                    .toList();

            matches.forEach(match -> log.info("岗位文本检索命中: {} (jobId={}, similarity={})",
                    match.job().getJobCategoryName(), match.job().getId(), match.similarity()));
            return result;
        } catch (Exception e) {
            log.error("岗位向量搜索失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getJobVector(Long jobId) {
        JobCategory job = jobCategoryMapper.selectById(jobId);
        if (job == null || job.getJobCategoryName() == null || job.getJobCategoryName().isBlank()) {
            log.warn("未找到岗位或岗位名称为空，jobId: {}", jobId);
            return null;
        }

        refreshJobEmbeddings(List.of(job));
        CachedJobEmbedding cached = embeddingCache.get(jobId);
        return cached == null ? null : vectorToString(cached.embedding());
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

    private JobSimilarity buildSimilarity(JobCategory job, float[] queryEmbedding) {
        CachedJobEmbedding cached = embeddingCache.get(job.getId());
        if (cached == null || cached.embedding() == null || cached.embedding().length != queryEmbedding.length) {
            return null;
        }

        double similarity = SimpleVectorStore.EmbeddingMath.cosineSimilarity(queryEmbedding, cached.embedding());
        if (Double.isNaN(similarity) || Double.isInfinite(similarity)) {
            return null;
        }
        return new JobSimilarity(job, similarity);
    }

    private void refreshJobEmbeddings(List<JobCategory> jobs) {
        List<JobCategory> pendingJobs = new ArrayList<>();
        for (JobCategory job : jobs) {
            if (job == null || job.getId() == null || job.getJobCategoryName() == null || job.getJobCategoryName().isBlank()) {
                continue;
            }
            CachedJobEmbedding cached = embeddingCache.get(job.getId());
            if (cached == null || !Objects.equals(cached.jobName(), job.getJobCategoryName())
                    || !Objects.equals(cached.updatedAt(), job.getUpdatedAt())) {
                pendingJobs.add(job);
            }
        }

        if (pendingJobs.isEmpty()) {
            return;
        }

        log.info("开始刷新岗位名称向量缓存，待刷新数量: {}", pendingJobs.size());
        for (int i = 0; i < pendingJobs.size(); i += EMBEDDING_BATCH_SIZE) {
            List<JobCategory> batch = pendingJobs.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, pendingJobs.size()));
            List<String> jobNames = batch.stream()
                    .map(JobCategory::getJobCategoryName)
                    .toList();
            List<float[]> embeddings = embeddingModel.embed(jobNames);
            for (int j = 0; j < batch.size(); j++) {
                JobCategory job = batch.get(j);
                float[] embedding = embeddings.get(j);
                if (embedding == null || embedding.length == 0) {
                    continue;
                }
                embeddingCache.put(job.getId(), new CachedJobEmbedding(
                        job.getJobCategoryName(),
                        job.getUpdatedAt(),
                        embedding
                ));
            }
        }
    }

    private String vectorToString(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(vector[i]);
        }
        builder.append("]");
        return builder.toString();
    }

    private record CachedJobEmbedding(String jobName, LocalDateTime updatedAt, float[] embedding) {
    }

    private record JobSimilarity(JobCategory job, double similarity) {
    }
}
