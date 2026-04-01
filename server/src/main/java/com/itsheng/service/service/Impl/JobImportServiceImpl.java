package com.itsheng.service.service.Impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.exception.FileUploadException;
import com.itsheng.pojo.entity.JobEntity;
import com.itsheng.pojo.entity.JobExcelRow;
import com.itsheng.pojo.entity.JobVectorStore;
import com.itsheng.service.mapper.JobMapper;
import com.itsheng.service.mapper.JobVectorStoreMapper;
import com.itsheng.service.service.JobImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 岗位导入 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobImportServiceImpl implements JobImportService {

    private final JobMapper jobMapper;
    private final JobVectorStoreMapper jobVectorStoreMapper;
    private final VectorStore jobVectorStore;
    private final OpenAiEmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    /**
     * 每批插入的记录数（每条记录 14 个字段，65535/14 ≈ 4680，为安全使用 1000）
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 向量化批次大小（当前 Embedding API 单次最多支持 10 条文本）
     */
    private static final int EMBEDDING_BATCH_SIZE = 10;

    @Override
    public void importFromExcel(MultipartFile file) {
        try {
            log.info("开始导入 Excel 岗位数据，文件名：{}", file.getOriginalFilename());

            // 校验文件
            validateFile(file);

            // 解析 Excel 数据
            List<JobEntity> jobs = parseExcel(file);
            log.info("Excel 解析成功，共{}条岗位数据", jobs.size());

            // 分批插入岗位数据
            if (!jobs.isEmpty()) {
                insertInBatches(jobs);
                log.info("岗位数据批量插入成功");

                // 异步批量处理向量化
                CompletableFuture.runAsync(() -> {
                    try {
                        processJobsInBatches(jobs);
                        log.info("岗位向量化处理完成");
                    } catch (Exception e) {
                        log.error("岗位向量化处理失败", e);
                    }
                });
            }

        } catch (Exception e) {
            log.error("Excel 岗位数据导入失败：{}", e.getMessage(), e);
            throw new FileUploadException("Excel 岗位数据导入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 批量处理岗位向量化
     */
    private void processJobsInBatches(List<JobEntity> jobs) {
        int totalSize = jobs.size();
        int batchCount = (totalSize + EMBEDDING_BATCH_SIZE - 1) / EMBEDDING_BATCH_SIZE;
        log.info("开始批量向量化处理，共{}条记录，分为{}批", totalSize, batchCount);

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * EMBEDDING_BATCH_SIZE;
            int toIndex = Math.min(fromIndex + EMBEDDING_BATCH_SIZE, totalSize);
            List<JobEntity> batchJobs = jobs.subList(fromIndex, toIndex);

            try {
                processEmbeddingBatch(batchJobs, i + 1, batchCount);
                log.info("已完成第 {}/{} 批向量化处理", i + 1, batchCount);
            } catch (Exception e) {
                log.error("第 {} 批向量化处理失败", i + 1, e);
            }
        }
    }

    /**
     * 处理一批岗位的向量化（使用 Spring AI VectorStore API）
     */
    private void processEmbeddingBatch(List<JobEntity> batchJobs, int batchNum, int totalBatches) {
        try {
            // 构建 Document 列表（Spring AI 会自动调用 Embedding 模型）
            List<Document> documents = new ArrayList<>();
            for (JobEntity job : batchJobs) {
                String vectorStoreId = UUID.randomUUID().toString();
                String jobContent = buildJobContent(job);
                Map<String, Object> metadata = buildMetadataMap(job);
                metadata.put("job_id", job.getId());
                metadata.put("vector_store_id", vectorStoreId);
                metadata.put("content_hash", generateContentHash(jobContent));

                Document document = Document.builder()
                        .id(vectorStoreId)
                        .text(jobContent)
                        .metadata(metadata)
                        .build();
                documents.add(document);
            }

            // 使用 VectorStore 添加文档（自动完成 embedding 并存储）
            jobVectorStore.add(documents);
            log.info("第 {}/{} 批：成功添加 {} 个岗位向量文档", batchNum, totalBatches, documents.size());

            // 批量更新 job_id 和 content_hash 字段（Spring AI 不会自动填充这些独立列）
            List<JobVectorStore> updates = new ArrayList<>();
            for (int i = 0; i < batchJobs.size(); i++) {
                JobEntity job = batchJobs.get(i);
                Document doc = documents.get(i);
                JobVectorStore jvs = JobVectorStore.builder()
                        .id(doc.getId())
                        .jobId(job.getId())
                        .contentHash((String) doc.getMetadata().get("content_hash"))
                        .build();
                updates.add(jvs);
            }
            jobVectorStoreMapper.updateBatch(updates);
            log.info("第 {}/{} 批：已更新 job_id 和 content_hash 字段", batchNum, totalBatches);

        } catch (Exception e) {
            log.error("第 {} 批向量化处理失败", batchNum, e);
        }
    }

    /**
     * 构建元数据 Map（用于 Document）
     */
    private Map<String, Object> buildMetadataMap(JobEntity job) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("job_name", job.getJobName());
        if (job.getJobDetail() != null) {
            metadata.put("job_detail", job.getJobDetail());
        }
        if (job.getCompanyName() != null) {
            metadata.put("company_name", job.getCompanyName());
        }
        if (job.getIndustry() != null) {
            metadata.put("industry", job.getIndustry());
        }
        if (job.getAddress() != null) {
            metadata.put("address", job.getAddress());
        }
        return metadata;
    }

    /**
     * 分批插入数据，避免超过 PostgreSQL 参数限制
     */
    private void insertInBatches(List<JobEntity> jobs) {
        int totalSize = jobs.size();
        int batchCount = (totalSize + BATCH_SIZE - 1) / BATCH_SIZE;

        for (int i = 0; i < batchCount; i++) {
            int fromIndex = i * BATCH_SIZE;
            int toIndex = Math.min(fromIndex + BATCH_SIZE, totalSize);
            List<JobEntity> batchJobs = jobs.subList(fromIndex, toIndex);

            // 每条记录开启一个事务
            insertBatchWithNewTransaction(batchJobs);

            log.info("已插入第 {}/{} 批，共{}条记录", i + 1, batchCount, batchJobs.size());
        }
    }

    /**
     * 插入单个批次（在新事务中）
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void insertBatchWithNewTransaction(List<JobEntity> jobs) {
        for (JobEntity job : jobs) {
            jobMapper.insert(job);
        }
    }

    /**
     * 校验上传的文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 检查文件大小（最大 50MB）
        if (file.getSize() > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持 50MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("无法获取文件名");
        }

        String fileType = getFileExtension(originalFilename);
        if (!"xls".equals(fileType) && !"xlsx".equals(fileType)) {
            throw new IllegalArgumentException("不支持的文件类型：" + fileType + "，请上传 Excel 格式文件 (.xls 或.xlsx)");
        }
    }

    /**
     * 从文件名获取扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("INVALID_FILE_NAME: 文件名不包含扩展名");
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 解析 Excel 文件
     */
    private List<JobEntity> parseExcel(MultipartFile file) throws IOException {
        List<JobEntity> jobs = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        EasyExcel.read(file.getInputStream(), JobExcelRow.class, new AnalysisEventListener<JobExcelRow>() {
            @Override
            public void invoke(JobExcelRow data, AnalysisContext context) {
                JobEntity job = convertToJobEntity(data, now);
                jobs.add(job);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel 解析完成");
            }

            @Override
            public void onException(Exception exception, AnalysisContext context) throws Exception {
                log.error("Excel 解析异常：{}", exception.getMessage());
                super.onException(exception, context);
            }
        }).sheet().doRead();

        return jobs;
    }

    /**
     * 将 Excel 行数据转换为 JobEntity
     */
    private JobEntity convertToJobEntity(JobExcelRow row, LocalDateTime now) {
        return JobEntity.builder()
                .jobName(row.getJobName())
                .address(row.getAddress())
                .salaryRange(row.getSalaryRange())
                .companyName(row.getCompanyName())
                .industry(row.getIndustry())
                .companySize(row.getCompanySize())
                .companyType(row.getCompanyType())
                .jobCode(row.getJobCode())
                .jobDetail(row.getJobDetail())
                .updateDate(row.getUpdateDate())
                .companyDetail(row.getCompanyDetail())
                .sourceUrl(row.getSourceUrl())
                .createTime(now)
                .updateTime(now)
                .build();
    }

    /**
     * 构建岗位内容（用于向量化）
     */
    private String buildJobContent(JobEntity job) {
        StringBuilder content = new StringBuilder();
        content.append("岗位名称：").append(job.getJobName()).append("\n");
        if (job.getAddress() != null) {
            content.append("工作地址：").append(job.getAddress()).append("\n");
        }
        if (job.getSalaryRange() != null) {
            content.append("薪资范围：").append(job.getSalaryRange()).append("\n");
        }
        if (job.getCompanyName() != null) {
            content.append("公司名称：").append(job.getCompanyName()).append("\n");
        }
        if (job.getIndustry() != null) {
            content.append("所属行业：").append(job.getIndustry()).append("\n");
        }
        if (job.getCompanySize() != null) {
            content.append("公司规模：").append(job.getCompanySize()).append("\n");
        }
        if (job.getCompanyType() != null) {
            content.append("公司类型：").append(job.getCompanyType()).append("\n");
        }
        if (job.getJobDetail() != null && !job.getJobDetail().isEmpty()) {
            content.append("岗位详情：\n").append(job.getJobDetail());
        }
        return content.toString();
    }

    /**
     * 生成内容哈希（用于去重）
     */
    private String generateContentHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("生成内容哈希失败", e);
            return UUID.randomUUID().toString();
        }
    }
}
