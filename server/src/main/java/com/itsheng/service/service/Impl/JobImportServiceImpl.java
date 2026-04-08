package com.itsheng.service.service.Impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.constant.SystemConstants;
import com.itsheng.common.exception.FileUploadException;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.JobEntity;
import com.itsheng.pojo.entity.JobExcelRow;
import com.itsheng.pojo.entity.JobVectorStore;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.JobMapper;
import com.itsheng.service.mapper.JobVectorStoreMapper;
import com.itsheng.service.service.JobImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 岗位导入 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class JobImportServiceImpl implements JobImportService {

    private final JobMapper jobMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final JobVectorStoreMapper jobVectorStoreMapper;
    private final VectorStore jobVectorStore;
    private final OpenAiEmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;
    private final ChatClient jobClassificationChatClient;

    /**
     * 每批插入的记录数（每条记录 14 个字段，65535/14 ≈ 4680，为安全使用 1000）
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 向量化批次大小（当前 Embedding API 单次最多支持 10 条文本）
     */
    private static final int EMBEDDING_BATCH_SIZE = 10;

    /**
     * AI 分类批次大小（避免 API 限流）
     */
    private static final int AI_CLASSIFICATION_BATCH_SIZE = 20;

    /**
     * 岗位级别枚举
     */
    private static final Map<String, String> LEVEL_NAME_MAP = Map.of(
            "INTERNSHIP", "实习岗",
            "JUNIOR", "初级岗",
            "MID", "中级岗",
            "SENIOR", "高级岗"
    );

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void classifyAndImportJobs() {
        log.info("========== 开始 AI 智能分类导入岗位数据 ==========");

        try {
            // 1. 按 ID 范围查询岗位数据（ID 20-1000，测试用）
            Long startId = 6001L;
            Long endId = 10000L;
            List<JobEntity> jobsInRange = jobMapper.selectByIdRange(startId, endId);
            if (jobsInRange == null || jobsInRange.isEmpty()) {
                log.warn("recruitment_data 表中 ID {}-{} 范围内暂无数据，无法进行分类", startId, endId);
                return;
            }
            log.info("查询到 ID {}-{} 范围内的 {} 条招聘数据", startId, endId, jobsInRange.size());

            // 2. AI 智能分类：逐条调用 AI 进行岗位分类
            Map<String, List<ClassificationResult>> classificationMap = new HashMap<>();
            int totalSize = jobsInRange.size();
            int batchCount = (totalSize + AI_CLASSIFICATION_BATCH_SIZE - 1) / AI_CLASSIFICATION_BATCH_SIZE;

            log.info("开始 AI 分类处理，共{}条记录，分为{}批，每批{}条", totalSize, batchCount, AI_CLASSIFICATION_BATCH_SIZE);

            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * AI_CLASSIFICATION_BATCH_SIZE;
                int toIndex = Math.min(fromIndex + AI_CLASSIFICATION_BATCH_SIZE, totalSize);
                List<JobEntity> batchJobs = jobsInRange.subList(fromIndex, toIndex);

                try {
                    processClassificationBatch(batchJobs, classificationMap, i + 1, batchCount);
                    log.info("已完成第 {}/{} 批 AI 分类处理", i + 1, batchCount);
                } catch (Exception e) {
                    log.error("第 {} 批 AI 分类处理失败", i + 1, e);
                }

                // 避免 API 限流，批次间短暂休眠
                if (i < batchCount - 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            // 3. 聚合相同类别 + 级别的记录，写入 job 表
            log.info("开始聚合分类结果，共{}个类别", classificationMap.size());
            int savedCount = aggregateAndSave(classificationMap);

            log.info("========== AI 智能分类导入完成，共保存{}条岗位分类记录 ==========", savedCount);

        } catch (Exception e) {
            log.error("AI 智能分类导入岗位数据失败", e);
            throw new RuntimeException("AI 智能分类导入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 处理一批岗位的 AI 分类
     */
    private void processClassificationBatch(List<JobEntity> batchJobs,
                                            Map<String, List<ClassificationResult>> classificationMap,
                                            int batchNum, int totalBatches) {
        for (JobEntity job : batchJobs) {
            try {
                ClassificationResult result = classifySingleJob(job);
                if (result != null) {
                    result.setSourceJobId(job.getId());
                    // 聚合 key：直接使用 AI 返回的 categoryCode（已包含行业前缀和级别后缀）
                    // 例如：BIO_RESEARCHER_JUNIOR, SEMI_TEST_ENG_MID, JAVA_DEV_JUNIOR
                    String key = result.getCategoryCode();
                    classificationMap.computeIfAbsent(key, k -> new ArrayList<>()).add(result);
                }
            } catch (Exception e) {
                log.error("岗位 AI 分类失败，jobId: {}, jobName: {}", job.getId(), job.getJobName(), e);
            }
        }
    }

    /**
     * 对单个岗位进行 AI 分类
     */
    private ClassificationResult classifySingleJob(JobEntity job) {
        try {
            // 构建岗位内容用于 AI 分析
            String jobContent = buildJobContentForClassification(job);

            // 调用 AI 进行分类
            String analysisJson = callAiForClassification(jobContent);

            // 解析 AI 返回的 JSON 结果
            return parseClassificationResult(analysisJson);

        } catch (Exception e) {
            log.error("AI 分类解析失败，jobId: {}", job.getId(), e);
            return null;
        }
    }

    /**
     * 构建用于分类的岗位内容
     */
    private String buildJobContentForClassification(JobEntity job) {
        StringBuilder content = new StringBuilder();
        content.append("岗位名称：").append(job.getJobName()).append("\n");
        if (job.getCity() != null) {
            content.append("工作城市：").append(job.getCity()).append("\n");
        }
        if (job.getSalaryRange() != null) {
            content.append("薪资范围：").append(job.getSalaryRange()).append("\n");
        }
        if (job.getJobDescription() != null && !job.getJobDescription().isEmpty()) {
            content.append("岗位职责：").append(job.getJobDescription()).append("\n");
        }
        if (job.getJobRequirements() != null && !job.getJobRequirements().isEmpty()) {
            content.append("任职要求：").append(job.getJobRequirements()).append("\n");
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
        if (job.getCompanyNature() != null) {
            content.append("公司性质：").append(job.getCompanyNature()).append("\n");
        }
        if (job.getCompanyDescription() != null && !job.getCompanyDescription().isEmpty()) {
            content.append("公司描述：").append(job.getCompanyDescription()).append("\n");
        }
        if (job.getJobDetail() != null && !job.getJobDetail().isEmpty()) {
            content.append("岗位详情：").append(job.getJobDetail()).append("\n");
        }
        if (job.getAddress() != null) {
            content.append("工作地址：").append(job.getAddress()).append("\n");
        }
        return content.toString();
    }

    /**
     * 调用 AI 进行岗位分类
     */
    private String callAiForClassification(String jobContent) {
        String userPrompt = String.format("""
            请分析以下招聘信息：

            ===== 招聘信息开始 =====
            %s
            ===== 招聘信息结束 =====

            请按照要求的 JSON 格式返回分类结果，直接返回 JSON 对象，不要包含任何 markdown 标记或额外说明。
            """, jobContent);

        ChatResponse response = jobClassificationChatClient.prompt()
                .system(SystemConstants.JOB_CLASSIFICATION_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    /**
     * 解析 AI 返回的分类结果
     */
    private ClassificationResult parseClassificationResult(String json) {
        try {
            // 清理可能存在的 markdown 代码块标记
            String cleanedJson = cleanJsonResponse(json);
            JsonNode rootNode = objectMapper.readTree(cleanedJson);

            ClassificationResult result = new ClassificationResult();
            result.setCategoryCode(rootNode.path("category_code").asText("UNKNOWN"));
            result.setCategoryName(rootNode.path("category_name").asText("未知岗位"));
            result.setLevel(rootNode.path("level").asText("MID"));
            result.setLevelName(rootNode.path("level_name").asText("中级岗"));
            result.setMinSalary(BigDecimal.valueOf(rootNode.path("min_salary").asDouble(0)));
            result.setMaxSalary(BigDecimal.valueOf(rootNode.path("max_salary").asDouble(0)));
            result.setSalaryUnit(rootNode.path("salary_unit").asText("MONTH"));
            result.setRequiredExperienceYears(rootNode.path("required_experience_years").asInt(0));

            // 解析 required_skills 数组
            List<String> skills = new ArrayList<>();
            JsonNode skillsNode = rootNode.path("required_skills");
            if (skillsNode.isArray()) {
                for (JsonNode skill : skillsNode) {
                    skills.add(skill.asText());
                }
            }
            result.setRequiredSkills(skills);

            result.setJobDescription(rootNode.path("job_description").asText(""));
            result.setConfidence(BigDecimal.valueOf(rootNode.path("confidence").asDouble(0.5)));

            return result;

        } catch (JsonProcessingException e) {
            log.error("解析分类结果 JSON 失败：{}", json, e);
            return null;
        }
    }

    /**
     * 清理 AI 返回的 JSON 响应
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "{}";
        }

        String cleaned = response.trim();

        // 移除开头的 ```json 或 ``` 标记
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // 移除结尾的 ``` 标记
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        // 找到第一个 { 和最后一个 } 之间的内容
        int startIndex = cleaned.indexOf('{');
        int endIndex = cleaned.lastIndexOf('}');

        if (startIndex >= 0 && endIndex > startIndex) {
            cleaned = cleaned.substring(startIndex, endIndex + 1);
        }

        return cleaned.trim();
    }

    /**
     * 聚合分类结果并保存到 job 表（支持 upsert：已存在则更新，不存在则插入）
     */
    private int aggregateAndSave(Map<String, List<ClassificationResult>> classificationMap) {
        int savedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<String, List<ClassificationResult>> entry : classificationMap.entrySet()) {
            List<ClassificationResult> results = entry.getValue();
            if (results == null || results.isEmpty()) {
                continue;
            }

            // 取第一条记录的基础信息
            ClassificationResult first = results.get(0);

            // 聚合本批次数据
            BigDecimal batchMinSalary = results.stream()
                    .map(ClassificationResult::getMinSalary)
                    .filter(Objects::nonNull)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            BigDecimal batchMaxSalary = results.stream()
                    .map(ClassificationResult::getMaxSalary)
                    .filter(Objects::nonNull)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);

            // 收集所有技能（去重）
            Set<String> allSkills = new HashSet<>();
            int totalExperienceYears = 0;
            int countWithExperience = 0;
            List<Long> batchSourceJobIds = new ArrayList<>();

            for (ClassificationResult r : results) {
                if (r.getRequiredSkills() != null) {
                    allSkills.addAll(r.getRequiredSkills());
                }
                if (r.getRequiredExperienceYears() > 0) {
                    totalExperienceYears += r.getRequiredExperienceYears();
                    countWithExperience++;
                }
                if (r.getSourceJobId() != null) {
                    batchSourceJobIds.add(r.getSourceJobId());
                }
            }

            // 计算平均经验要求
            int avgExperienceYears = countWithExperience > 0
                    ? totalExperienceYears / countWithExperience
                    : 0;

            // 计算平均置信度
            BigDecimal avgConfidence = results.stream()
                    .map(ClassificationResult::getConfidence)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(results.size()), 2, BigDecimal.ROUND_HALF_UP);

            // 将 List 转换为 JSON 字符串
            String batchSourceJobIdsJson;
            String requiredSkillsJson;
            try {
                batchSourceJobIdsJson = objectMapper.writeValueAsString(batchSourceJobIds);
                requiredSkillsJson = objectMapper.writeValueAsString(new ArrayList<>(allSkills));
            } catch (JsonProcessingException e) {
                log.error("序列化 JSON 失败", e);
                continue;
            }

            // UPSERT 逻辑：先查询是否已存在该类别的记录（categoryCode 已包含级别后缀）
            // 例如：BIO_RESEARCHER_JUNIOR, SEMI_TEST_ENG_MID, JAVA_DEV_JUNIOR
            JobCategory existing = jobCategoryMapper.selectByFullCategoryCode(first.getCategoryCode());

            if (existing != null) {
                // 已存在，执行 UPDATE：无论薪资是否变化，都要合并 source_job_ids 和 skills
                BigDecimal newMinSalary = existing.getMinSalary().compareTo(batchMinSalary) < 0
                        ? existing.getMinSalary() : batchMinSalary;
                BigDecimal newMaxSalary = existing.getMaxSalary().compareTo(batchMaxSalary) > 0
                        ? existing.getMaxSalary() : batchMaxSalary;

                // 合并 source_job_ids（即使薪资不变也要拼接）
                List<Long> mergedSourceJobIds = parseJobIds(existing.getSourceJobIds());
                int newJobCount = batchSourceJobIds.size();
                mergedSourceJobIds.addAll(batchSourceJobIds);
                // 去重
                mergedSourceJobIds = new ArrayList<>(new LinkedHashSet<>(mergedSourceJobIds));

                // 合并 required_skills
                Set<String> mergedSkills = parseSkills(existing.getRequiredSkills());
                mergedSkills.addAll(allSkills);

                String mergedSourceJobIdsJson;
                String mergedSkillsJson;
                try {
                    mergedSourceJobIdsJson = objectMapper.writeValueAsString(mergedSourceJobIds);
                    mergedSkillsJson = objectMapper.writeValueAsString(new ArrayList<>(mergedSkills));
                } catch (JsonProcessingException e) {
                    log.error("序列化合并后的 JSON 失败", e);
                    continue;
                }

                // 重新计算平均置信度（考虑已有记录）
                BigDecimal newAvgConfidence = existing.getAiConfidenceScore()
                        .add(avgConfidence.multiply(BigDecimal.valueOf(results.size())))
                        .divide(BigDecimal.valueOf(existing.getSourceJobCount() + results.size()), 2, BigDecimal.ROUND_HALF_UP);

                JobCategory updateEntity = JobCategory.builder()
                        .id(existing.getId())
                        .minSalary(newMinSalary)
                        .maxSalary(newMaxSalary)
                        .sourceJobIds(mergedSourceJobIdsJson)
                        .sourceJobCount(mergedSourceJobIds.size())
                        .requiredSkills(mergedSkillsJson)
                        .aiConfidenceScore(newAvgConfidence)
                        .updatedAt(now)
                        .build();

                int updated = jobCategoryMapper.update(updateEntity);
                if (updated > 0) {
                    savedCount++;
                    log.info("已更新岗位分类记录 ID={}, 类别={}, 级别={}, 薪资范围 {}-{}, 新增{}条源岗位数据（合并后共{}条）",
                            existing.getId(), existing.getJobCategoryCode(), existing.getJobLevel(),
                            newMinSalary, newMaxSalary, newJobCount, mergedSourceJobIds.size());
                }

            } else {
                // 不存在，执行 INSERT
                JobCategory jobCategory = JobCategory.builder()
                        .jobCategoryCode(first.getCategoryCode())
                        .jobCategoryName(first.getCategoryName())
                        .jobLevel(first.getLevel())
                        .jobLevelName(LEVEL_NAME_MAP.getOrDefault(first.getLevel(), first.getLevelName()))
                        .minSalary(batchMinSalary)
                        .maxSalary(batchMaxSalary)
                        .salaryUnit(first.getSalaryUnit())
                        .sourceJobIds(batchSourceJobIdsJson)
                        .sourceJobCount(batchSourceJobIds.size())
                        .requiredExperienceYears(avgExperienceYears)
                        .requiredSkills(requiredSkillsJson)
                        .jobDescription(first.getJobDescription())
                        .aiConfidenceScore(avgConfidence)
                        .analysisPromptUsed("JOB_CLASSIFICATION_PROMPT")
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                int inserted = jobCategoryMapper.insert(jobCategory);
                if (inserted > 0) {
                    savedCount++;
                    log.info("已插入岗位分类记录 ID={}, 类别={}, 级别={}, 包含{}条源岗位数据",
                            jobCategory.getId(), jobCategory.getJobCategoryCode(), jobCategory.getJobLevel(), batchSourceJobIds.size());
                }
            }
        }

        log.info("聚合保存完成，共{}条记录（含新增和更新）", savedCount);
        return savedCount;
    }

    /**
     * 解析 source_job_ids JSON 字符串为 List
     */
    private List<Long> parseJobIds(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return new ArrayList<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                List<Long> result = new ArrayList<>();
                for (JsonNode idNode : node) {
                    result.add(idNode.asLong());
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析 source_job_ids 失败：{}", json, e);
        }
        return new ArrayList<>();
    }

    /**
     * 解析 required_skills JSON 字符串为 Set
     */
    private Set<String> parseSkills(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json.trim())) {
            return new HashSet<>();
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node.isArray()) {
                Set<String> result = new HashSet<>();
                for (JsonNode skillNode : node) {
                    result.add(skillNode.asText());
                }
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析 required_skills 失败：{}", json, e);
        }
        return new HashSet<>();
    }

    /**
     * 内部类：用于临时存储单个岗位的分类结果
     */
    private static class ClassificationResult {
        private String categoryCode;
        private String categoryName;
        private String level;
        private String levelName;
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private String salaryUnit;
        private Integer requiredExperienceYears;
        private List<String> requiredSkills;
        private String jobDescription;
        private BigDecimal confidence;
        private Long sourceJobId;  // 关联的原始岗位 ID

        // Getters and Setters
        public String getCategoryCode() { return categoryCode; }
        public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }

        public String getLevelName() { return levelName; }
        public void setLevelName(String levelName) { this.levelName = levelName; }

        public BigDecimal getMinSalary() { return minSalary; }
        public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

        public BigDecimal getMaxSalary() { return maxSalary; }
        public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

        public String getSalaryUnit() { return salaryUnit; }
        public void setSalaryUnit(String salaryUnit) { this.salaryUnit = salaryUnit; }

        public Integer getRequiredExperienceYears() { return requiredExperienceYears; }
        public void setRequiredExperienceYears(Integer requiredExperienceYears) { this.requiredExperienceYears = requiredExperienceYears; }

        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

        public String getJobDescription() { return jobDescription; }
        public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

        public BigDecimal getConfidence() { return confidence; }
        public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

        public Long getSourceJobId() { return sourceJobId; }
        public void setSourceJobId(Long sourceJobId) { this.sourceJobId = sourceJobId; }
    }

    /**
     * 整合 job 表中 job_category_code 字段
     * 检测编码字段，若没有级别后缀（INTERNSHIP/JUNIOR/MID/SENIOR），
     * 通过 job_level 字段给它加上等级后缀。
     * 若更新后发现与已有记录重复，则合并数据并删除原记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void consolidateJobCategoryCodes() {
        log.info("========== 开始整合 job 表岗位类别编码 ==========");

        try {
            // 1. 查询所有岗位分类记录
            List<JobCategory> allJobs = jobCategoryMapper.selectAll();
            if (allJobs == null || allJobs.isEmpty()) {
                log.warn("job 表中暂无数据，无法进行整合");
                return;
            }
            log.info("共查询到 {} 条岗位分类记录", allJobs.size());

            // 2. 统计需要处理的记录
            int processedCount = 0;
            int mergedCount = 0;
            int updatedCount = 0;

            // 3. 遍历所有记录
            for (JobCategory job : allJobs) {
                String categoryCode = job.getJobCategoryCode();
                String level = job.getJobLevel();

                // 跳过已有级别后缀的记录
                if (hasLevelSuffix(categoryCode)) {
                    log.debug("记录 ID={} 的编码 {} 已包含级别后缀，跳过", job.getId(), categoryCode);
                    continue;
                }

                // 4. 生成新的编码（基础编码 + 级别后缀）
                String newCategoryCode = appendLevelSuffix(categoryCode, level);
                log.info("记录 ID={} 原编码 {} 无级别后缀，将更新为 {}", job.getId(), categoryCode, newCategoryCode);

                // 5. 检查新编码是否已存在
                JobCategory existing = jobCategoryMapper.selectByFullCategoryCode(newCategoryCode);

                if (existing != null) {
                    // 已存在，合并数据并删除原记录
                    mergeJobCategories(existing, job);
                    mergedCount++;
                    log.info("记录 ID={} 与 ID={} 合并（新编码 {} 已存在），已删除原记录",
                            job.getId(), existing.getId(), newCategoryCode);
                } else {
                    // 不存在，直接更新当前记录
                    JobCategory updateEntity = JobCategory.builder()
                            .id(job.getId())
                            .jobCategoryCode(newCategoryCode)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    jobCategoryMapper.update(updateEntity);
                    updatedCount++;
                    log.info("记录 ID={} 编码已更新为 {}", job.getId(), newCategoryCode);
                }

                processedCount++;
            }

            log.info("========== 整合完成，共处理 {} 条记录：更新 {} 条，合并 {} 条 ==========",
                    processedCount, updatedCount, mergedCount);

        } catch (Exception e) {
            log.error("整合 job 表岗位类别编码失败", e);
            throw new RuntimeException("整合失败：" + e.getMessage(), e);
        }
    }

    @Override
    public void generateJobProfiles() {
        log.info("开始为所有岗位生成职业画像");
        
        List<JobCategory> allJobs = jobCategoryMapper.selectAll();
        log.info("找到 {} 个岗位需要生成画像", allJobs.size());
        
        int updated = 0;
        for (JobCategory job : allJobs) {
            try {
                Map<String, Object> profile = buildJobProfile(job);
                String profileJson = objectMapper.writeValueAsString(profile);
                
                JobCategory updateEntity = JobCategory.builder()
                        .id(job.getId())
                        .jobProfile(profileJson)
                        .updatedAt(LocalDateTime.now())
                        .build();
                jobCategoryMapper.update(updateEntity);
                updated++;
                
                if (updated % 50 == 0) {
                    log.info("已为 {} 个岗位生成画像", updated);
                }
            } catch (Exception e) {
                log.error("为岗位 {} 生成画像失败: {}", job.getId(), e.getMessage());
            }
        }
        
        log.info("岗位画像生成完成。已更新 {} 个岗位", updated);
    }
    
    /**
     * 根据岗位数据构建画像Map
     */
    private Map<String, Object> buildJobProfile(JobCategory job) {
        Map<String, Object> profile = new LinkedHashMap<>();
        
        // 根据类别编码推导行业领域
        profile.put("industrySegment", deriveIndustrySegment(job.getJobCategoryCode()));
        
        // 城市（占位符 - 如需要可从 source_job_ids 推导）
        profile.put("city", deriveCity(job));
        
        // 从 requiredSkills JSON 获取核心技能
        List<String> coreSkills = parseSkillsAsList(job.getRequiredSkills());
        profile.put("coreSkills", coreSkills);
        
        // 根据岗位类型推导证书要求
        profile.put("certificateRequirements", deriveCertificates(job.getJobCategoryCode()));
        
        // 能力需求（创新、学习、抗压、沟通、实习）
        Map<String, Integer> capabilities = deriveCapabilities(job);
        profile.put("capabilityRequirements", capabilities);
        
        // 根据源岗位数量计算需求等级
        profile.put("demandLevel", deriveDemandLevel(job.getSourceJobCount()));
        
        // 热门岗位亮点
        List<String> highlights = deriveHighlights(job);
        profile.put("highlights", highlights);
        
        return profile;
    }
    
    /**
     * 根据类别编码推导行业领域
     */
    private String deriveIndustrySegment(String categoryCode) {
        if (categoryCode == null) return "技术";
        String code = categoryCode.toUpperCase();
        if (code.contains("JAVA") || code.contains("BACKEND") || code.contains("SPRING")) {
            return "后端开发";
        }
        if (code.contains("FRONTEND") || code.contains("WEB") || code.contains("VUE") || code.contains("REACT")) {
            return "前端开发";
        }
        if (code.contains("DATA") || code.contains("BIGDATA") || code.contains("ETL")) {
            return "数据工程";
        }
        if (code.contains("AI") || code.contains("ML") || code.contains("MACHINE") || code.contains("ALGORITHM")) {
            return "AI/机器学习";
        }
        if (code.contains("DEVOPS") || code.contains("OPS") || code.contains("SRE")) {
            return "DevOps";
        }
        if (code.contains("MOBILE") || code.contains("ANDROID") || code.contains("IOS") || code.contains("FLUTTER")) {
            return "移动开发";
        }
        if (code.contains("TEST") || code.contains("QA")) {
            return "质量保障";
        }
        if (code.contains("PRODUCT") || code.contains("PM")) {
            return "产品管理";
        }
        if (code.contains("DESIGN") || code.contains("UI") || code.contains("UX")) {
            return "设计";
        }
        if (code.contains("SECURITY")) {
            return "安全";
        }
        if (code.contains("EMBEDDED") || code.contains("HARDWARE")) {
            return "嵌入式系统";
        }
        return "技术";
    }
    
    /**
     * 推导城市
     */
    private String deriveCity(JobCategory job) {
        // 根据岗位特征推导默认城市
        if (job.getSourceJobCount() != null && job.getSourceJobCount() > 50) {
            return "北京/上海/深圳";
        }
        return "北京";
    }
    
    /**
     * 解析技能JSON为列表
     */
    private List<String> parseSkillsAsList(String skillsJson) {
        if (skillsJson == null || skillsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(skillsJson, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析技能JSON失败: {}", skillsJson);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据类别编码推导证书要求
     */
    private List<String> deriveCertificates(String categoryCode) {
        List<String> certs = new ArrayList<>();
        if (categoryCode == null) {
            certs.add("英语四级及以上");
            return certs;
        }
        String code = categoryCode.toUpperCase();
        if (code.contains("JAVA")) {
            certs.add("Oracle认证Java程序员");
            certs.add("Spring专业认证");
        } else if (code.contains("FRONTEND") || code.contains("WEB")) {
            certs.add("AWS认证开发者");
        } else if (code.contains("DATA")) {
            certs.add("AWS认证数据分析");
            certs.add("CDMP（认证数据管理专业人员）");
        } else if (code.contains("AI") || code.contains("ML")) {
            certs.add("TensorFlow开发者证书");
            certs.add("AWS机器学习专项认证");
        } else if (code.contains("DEVOPS")) {
            certs.add("AWS DevOps工程师");
            certs.add("Kubernetes管理员(CKA)");
        } else if (code.contains("SECURITY")) {
            certs.add("CISSP");
            certs.add("CEH（认证道德黑客）");
        } else if (code.contains("PRODUCT")) {
            certs.add("PMP");
            certs.add("CSPO（认证Scrum产品负责人）");
        }
        certs.add("英语四级及以上");
        return certs;
    }
    
    /**
     * 推导能力需求分数
     */
    private Map<String, Integer> deriveCapabilities(JobCategory job) {
        Map<String, Integer> caps = new LinkedHashMap<>();
        String level = job.getJobLevel();
        int baseScore;
        
        if ("SENIOR".equals(level)) {
            baseScore = 75;
        } else if ("MID".equals(level)) {
            baseScore = 65;
        } else if ("JUNIOR".equals(level)) {
            baseScore = 55;
        } else {
            baseScore = 50;
        }
        
        // 根据岗位类型添加一些变化
        String code = job.getJobCategoryCode() != null ? job.getJobCategoryCode().toUpperCase() : "";
        int innovationBonus = code.contains("AI") || code.contains("DESIGN") ? 10 : 5;
        int learningBonus = code.contains("AI") || code.contains("DATA") ? 10 : 5;
        int communicationBonus = code.contains("PRODUCT") || code.contains("PM") ? 10 : 5;
        int internshipBonus = code.contains("JUNIOR") || code.contains("INTERNSHIP") ? 10 : 5;
        
        caps.put("innovation", Math.min(100, baseScore + innovationBonus));
        caps.put("learning", Math.min(100, baseScore + learningBonus));
        caps.put("resilience", baseScore + 5);
        caps.put("communication", Math.min(100, baseScore + communicationBonus));
        caps.put("internship", Math.min(100, baseScore + internshipBonus));
        
        return caps;
    }
    
    /**
     * 根据源岗位数量推导需求等级
     */
    private String deriveDemandLevel(Integer sourceJobCount) {
        if (sourceJobCount == null || sourceJobCount < 10) {
            return "低";
        } else if (sourceJobCount < 30) {
            return "中";
        } else if (sourceJobCount < 60) {
            return "高";
        } else {
            return "极高";
        }
    }
    
    /**
     * 推导岗位亮点
     */
    private List<String> deriveHighlights(JobCategory job) {
        List<String> highlights = new ArrayList<>();
        
        if (job.getSourceJobCount() != null && job.getSourceJobCount() > 50) {
            highlights.add("市场需求旺盛");
        }
        if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(new BigDecimal("30000")) > 0) {
            highlights.add("薪资竞争力强");
        }
        if (job.getAiConfidenceScore() != null && job.getAiConfidenceScore().compareTo(new BigDecimal("0.9")) > 0) {
            highlights.add("AI分类置信度高");
        }
        
        String code = job.getJobCategoryCode() != null ? job.getJobCategoryCode().toUpperCase() : "";
        if (code.contains("AI") || code.contains("ML")) {
            highlights.add("新兴技术领域");
        }
        if (code.contains("SENIOR")) {
            highlights.add("高级岗位机会");
        }
        
        if (highlights.isEmpty()) {
            highlights.add("稳定职业路径");
        }
        
        return highlights;
    }
    
    /**
     * 检查类别编码是否已有级别后缀
     */
    private boolean hasLevelSuffix(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return code.endsWith("_INTERNSHIP") ||
               code.endsWith("_JUNIOR") ||
               code.endsWith("_MID") ||
               code.endsWith("_SENIOR");
    }

    /**
     * 给基础编码添加级别后缀
     * @param baseCode 基础编码（如 CPP_DEV）
     * @param level 级别（如 INTERNSHIP）
     * @return 完整编码（如 CPP_DEV_INTERNSHIP）
     */
    private String appendLevelSuffix(String baseCode, String level) {
        if (baseCode == null || level == null) {
            return baseCode;
        }
        // 确保级别是有效的
        if (!"INTERNSHIP".equals(level) &&
            !"JUNIOR".equals(level) &&
            !"MID".equals(level) &&
            !"SENIOR".equals(level)) {
            log.warn("无效的级别值：{}，将返回原编码", level);
            return baseCode;
        }
        return baseCode + "_" + level;
    }

    /**
     * 合并两条岗位分类记录
     * 将 sourceRecord 的数据合并到 targetRecord，然后删除 sourceRecord
     * @param target 目标记录（已存在的记录）
     * @param source 源记录（需要被合并删除的记录）
     */
    private void mergeJobCategories(JobCategory target, JobCategory source) {
        try {
            // 1. 合并薪资范围（取最小值和最大值）
            BigDecimal newMinSalary = target.getMinSalary().compareTo(source.getMinSalary()) < 0
                    ? target.getMinSalary() : source.getMinSalary();
            BigDecimal newMaxSalary = target.getMaxSalary().compareTo(source.getMaxSalary()) > 0
                    ? target.getMaxSalary() : source.getMaxSalary();

            // 2. 合并 source_job_ids
            List<Long> mergedSourceJobIds = parseJobIds(target.getSourceJobIds());
            mergedSourceJobIds.addAll(parseJobIds(source.getSourceJobIds()));
            // 去重
            mergedSourceJobIds = new ArrayList<>(new LinkedHashSet<>(mergedSourceJobIds));

            // 3. 合并 required_skills
            Set<String> mergedSkills = parseSkills(target.getRequiredSkills());
            mergedSkills.addAll(parseSkills(source.getRequiredSkills()));

            String mergedSourceJobIdsJson = objectMapper.writeValueAsString(mergedSourceJobIds);
            String mergedSkillsJson = objectMapper.writeValueAsString(new ArrayList<>(mergedSkills));

            // 4. 计算新的平均置信度
            BigDecimal newAvgConfidence = target.getAiConfidenceScore()
                    .multiply(BigDecimal.valueOf(target.getSourceJobCount()))
                    .add(source.getAiConfidenceScore().multiply(BigDecimal.valueOf(source.getSourceJobCount())))
                    .divide(BigDecimal.valueOf(target.getSourceJobCount() + source.getSourceJobCount()), 2, BigDecimal.ROUND_HALF_UP);

            // 5. 更新目标记录
            JobCategory updateEntity = JobCategory.builder()
                    .id(target.getId())
                    .minSalary(newMinSalary)
                    .maxSalary(newMaxSalary)
                    .sourceJobIds(mergedSourceJobIdsJson)
                    .sourceJobCount(mergedSourceJobIds.size())
                    .requiredSkills(mergedSkillsJson)
                    .aiConfidenceScore(newAvgConfidence)
                    .updatedAt(LocalDateTime.now())
                    .build();
            jobCategoryMapper.update(updateEntity);

            // 6. 删除源记录
            jobCategoryMapper.deleteById(source.getId());

        } catch (JsonProcessingException e) {
            log.error("合并岗位分类记录失败", e);
            throw new RuntimeException("合并失败：" + e.getMessage(), e);
        }
    }
}
