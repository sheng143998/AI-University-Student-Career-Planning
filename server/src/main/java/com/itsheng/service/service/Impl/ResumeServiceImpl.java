package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.constant.ResumeConstant;
import com.itsheng.common.constant.SystemConstants;
import com.itsheng.common.exception.FileUploadException;
import com.itsheng.common.exception.FileNotFoundException;
import com.itsheng.common.exception.ResumeAnalysisException;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.dto.ResumeScores;
import com.itsheng.pojo.dto.ResumeSuggestion;
import com.itsheng.pojo.entity.JobCategory;
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.StudentCapabilityProfile;
import com.itsheng.pojo.entity.UserCareerData;
import com.itsheng.pojo.entity.UserRoadmapSteps;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.pojo.vo.CapabilityProfileVO;
import com.itsheng.pojo.vo.ResumeAnalysisResultVO;
import com.itsheng.pojo.vo.ResumeUploadVO;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.JobCategoryMapper;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.StudentCapabilityProfileMapper;
import com.itsheng.service.mapper.UserCareerDataMapper;
import com.itsheng.service.mapper.UserRoadmapStepsMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.JobVectorSearchService;
import com.itsheng.service.service.ResumeOcrService;
import com.itsheng.service.service.ResumeService;
import com.itsheng.common.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final VectorStore pgVectorStore;
    private final OpenAiEmbeddingModel embeddingModel;
    private final CommonController commonController;
    private final UserVectorStoreMapper userVectorStoreMapper;
    private final ResumeMapper resumeMapper;
    private final StudentCapabilityProfileMapper capabilityProfileMapper;
    private final AliOssUtil aliOssUtil;
    private final ChatClient resumeAnalysisChatClient;
    private final ObjectMapper objectMapper;
    private final JobCategoryMapper jobCategoryMapper;
    private final JobVectorSearchService jobVectorSearchService;
    private final UserCareerDataMapper userCareerDataMapper;
    private final UserRoadmapStepsMapper userRoadmapStepsMapper;
    private final ResumeOcrService resumeOcrService;

    @Value("${resume.parser.pdf.min-effective-chars:80}")
    private int pdfMinEffectiveChars;

    @Value("${resume.parser.pdf.min-effective-chars-per-page:20}")
    private int pdfMinEffectiveCharsPerPage;

    @Value("${resume.parser.pdf.ocr-enabled:true}")
    private boolean pdfOcrEnabled;

    // 文件类型与 MIME 类型的映射
    private static final Map<String, String> MIME_TYPE_MAP = new HashMap<>();

    static {
        MIME_TYPE_MAP.put("pdf", "application/pdf");
        MIME_TYPE_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        MIME_TYPE_MAP.put("doc", "application/msword");
        MIME_TYPE_MAP.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        MIME_TYPE_MAP.put("ppt", "application/vnd.ms-powerpoint");
        MIME_TYPE_MAP.put("html", "text/html");
        MIME_TYPE_MAP.put("htm", "text/html");
        MIME_TYPE_MAP.put("txt", "text/plain");
        MIME_TYPE_MAP.put("md", "text/plain; charset=UTF-8");
    }

    // 支持内联预览的文件类型
    private static final Set<String> INLINE_PREVIEW_TYPES = Set.of("pdf", "html", "htm", "txt", "md");

    @Override
    @Transactional
    public ResumeUploadVO upload(MultipartFile file) {
        try {
            // 获取当前用户 ID
            Long userId = BaseContext.getUserId();
            log.info("开始解析用户 {} 的简历文件", userId);

            // 校验文件
            validateFile(file);

            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            String fileType = getFileExtension(originalFilename);
            log.debug("简历上传参数：userId={}, originalFilename={}, fileType={}, fileSize={} bytes",
                    userId, originalFilename, fileType, file.getSize());

            // 调用 CommonController 上传文件到 OSS
            Result<String> uploadResult = commonController.upload(file);
            if (uploadResult == null || uploadResult.getData() == null) {
                throw new FileUploadException("文件上传到 OSS 失败");
            }
            String fileUrl = uploadResult.getData();
            log.info("文件已上传到 OSS: {}", fileUrl);

            // 解析简历内容
            byte[] fileBytes = file.getBytes();
            List<Document> documents = parseResume(fileBytes, fileType, userId, fileUrl);
            log.debug("简历解析完成：userId={}, fileType={}, documentCount={}", userId, fileType, documents.size());

            if (documents.isEmpty()) {
                log.warn("简历解析后未生成任何文档片段，终止后续流程，userId={}, fileUrl={}", userId, fileUrl);
                throw new FileUploadException("RESUME_TEXT_EXTRACTION_FAILED: 简历未识别到有效内容，请上传可编辑文本版 PDF 或 DOCX");
            }

            // 将简历内容分批添加到向量存储（通义千问限制 batch size 最大为 10）
            int batchSize = 10;
            int totalDocuments = documents.size();
            for (int i = 0; i < totalDocuments; i += batchSize) {
                List<Document> batch = documents.subList(i, Math.min(i + batchSize, totalDocuments));
                pgVectorStore.add(batch);
                log.info("已添加第 {} 批文档向量 ({}-{} / {})", (i / batchSize) + 1, i + 1, Math.min(i + batchSize, totalDocuments), totalDocuments);
            }
            log.info("简历向量已成功添加到向量存储，共{}个文档片段", documents.size());

            // 使用 Mapper 批量更新 user_id 和 resume_file_path 到数据库
            for (Document doc : documents) {
                UserVectorStore userVectorStore = UserVectorStore.builder()
                        .id(doc.getId())
                        .userId(userId)
                        .resumeFilePath(fileUrl)
                        .build();
                userVectorStoreMapper.update(userVectorStore);
            }
            log.info("已更新 user_id 和 resume_file_path 到数据库，userId: {}, filePath: {}", userId, fileUrl);

            // 合并所有内容用于返回
            String resumeContent = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));
            int effectiveChars = countEffectiveChars(resumeContent);
            log.debug("简历文本聚合完成：userId={}, totalChars={}, effectiveChars={}", userId,
                    resumeContent.length(), effectiveChars);

            if (effectiveChars == 0) {
                log.warn("简历聚合文本为空，终止 AI 分析流程，userId={}, fileUrl={}", userId, fileUrl);
                throw new FileUploadException("RESUME_TEXT_EXTRACTION_FAILED: 简历内容为空，无法继续分析");
            }

            // 生成主键 ID（UUID 字符串）
            String id = documents.get(0).getId();

            // 异步调用 AI 进行简历解析
            String finalOriginalFilename = originalFilename;
            CompletableFuture.runAsync(() -> {
                try {
                    analyzeAndSave(id, userId, resumeContent, fileType, finalOriginalFilename, fileUrl);
                } catch (Exception e) {
                    log.error("异步简历 AI 解析失败，vectorStoreId: {}", id, e);
                }
            });

            // 返回 UploadVO
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ResumeUploadVO.builder()
                    .id(id)
                    .userId(String.valueOf(userId))
                    .resumeFilePath(fileUrl)
                    .parsingStatus(ResumeConstant.PARSING_STATUS_PROCESSING)
                    .createdAt(now)
                    .build();

        } catch (Exception e) {
            log.error("简历上传解析失败：{}", e.getMessage(), e);
            throw new FileUploadException("简历上传解析失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void analyzeAndSave(String vectorStoreId, Long userId, String resumeContent,
                               String fileType, String originalFileName, String resumeFilePath) {
        try {
            log.info("开始 AI 解析简历，vectorStoreId: {}, userId: {}", vectorStoreId, userId);

            // 1. 先插入一条初始记录，状态为 processing，进度为 0
            ResumeAnalysisResult initialRecord = ResumeAnalysisResult.builder()
                    .vectorStoreId(vectorStoreId)
                    .userId(userId)
                    .fileType(fileType)
                    .originalFileName(originalFileName)
                    .resumeFilePath(resumeFilePath)
                    .status("processing")
                    .progress(0)
                    .parsedData("{}")
                    .scores("{\"keyword_match\":0,\"layout\":0,\"skill_depth\":0,\"experience\":0}")
                    .highlights("[]")
                    .suggestions("[]")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            resumeMapper.insert(initialRecord);
            log.info("已插入初始分析记录，analysisId: {}, resumeFilePath: {}", initialRecord.getId(), resumeFilePath);

            // 2. 启动异步进度更新任务（每 5 秒更新一次进度）
            ProgressTracker tracker = new ProgressTracker();
            tracker.startTracking(vectorStoreId);

            // 3. 匹配用户目标岗位
            JobCategory targetJob = matchTargetJob(vectorStoreId, resumeContent);
            String targetJobName = targetJob != null ? targetJob.getJobCategoryName() : null;
            Long targetJobId = targetJob != null ? targetJob.getId() : null;
            log.info("目标岗位匹配完成: {}, jobId: {}", targetJobName, targetJobId);

            // 4. 调用 AI 进行简历解析（结合目标岗位）
            String analysisJson = callAiForAnalysis(resumeContent, targetJob);
            log.info("AI 返回的解析结果：{}", analysisJson);

            // 5. 停止进度更新
            tracker.stopTracking();

            // 6. 解析 JSON 并提取各字段
            JsonAnalysisResult analysisResult = parseAnalysisJson(analysisJson);

            // 7. 更新分析结果记录为完成状态
            ResumeAnalysisResult analysisRecord = ResumeAnalysisResult.builder()
                    .id(initialRecord.getId())
                    .vectorStoreId(vectorStoreId)
                    .userId(userId)
                    .fileType(fileType)
                    .originalFileName(originalFileName)
                    .status("completed")
                    .progress(100)
                    .parsedData(analysisResult.parsedDataJson)
                    .scores(analysisResult.scoresJson)
                    .highlights(analysisResult.highlightsJson)
                    .suggestions(analysisResult.suggestionsJson)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // 8. 更新数据库
            resumeMapper.update(analysisRecord);
            log.info("简历分析结果已更新，analysisId: {}", analysisRecord.getId());

            // 9. 调用 AI 生成学生能力画像并保存（结合目标岗位）
            try {
                generateCapabilityProfile(userId, analysisRecord.getId(), resumeContent, targetJob);
            } catch (Exception e) {
                log.error("生成学生能力画像失败，userId: {}, analysisId: {}", userId, analysisRecord.getId(), e);
            }

            // 10. 更新 user_vector_store 表的状态为 COMPLETED
            UserVectorStore userVectorStore = UserVectorStore.builder()
                    .id(vectorStoreId)
                    .resumeContent(resumeContent)
                    .build();
            userVectorStoreMapper.update(userVectorStore);

            // 11. 自动生成 user_career_data（Dashboard 数据）
            try {
                generateUserCareerData(userId, targetJob, targetJobName, targetJobId, analysisRecord);
            } catch (Exception e) {
                log.error("生成用户职业数据失败，userId: {}", userId, e);
            }

            log.info("简历解析完成，vectorStoreId: {}", vectorStoreId);

        } catch (Exception e) {
            log.error("简历 AI 解析失败：{}", e.getMessage(), e);
            // 更新状态为 failed
            try {
                ResumeAnalysisResult failedRecord = ResumeAnalysisResult.builder()
                        .vectorStoreId(vectorStoreId)
                        .status("failed")
                        .progress(0)
                        .build();
                resumeMapper.update(failedRecord);
            } catch (Exception ex) {
                log.error("更新失败状态失败：{}", ex.getMessage());
            }
            throw new ResumeAnalysisException("简历 AI 解析失败：" + e.getMessage(), e);
        }
    }

    @Override
    public ResumeAnalysisResultVO getAnalysisResult(String vectorStoreId) {
        // 1. 查询分析结果
        ResumeAnalysisResult result = resumeMapper.selectByVectorStoreId(vectorStoreId);

        // 2. 如果尚未分析完成，返回 PROCESSING 状态
        if (result == null) {
            // 尝试从 user_vector_store 获取基本信息
            UserVectorStore store = userVectorStoreMapper.selectByVectorStoreId(vectorStoreId);
            if (store != null) {
                return ResumeAnalysisResultVO.builder()
                        .vectorStoreId(vectorStoreId)
                        .userId(store.getUserId())
                        .resumeFilePath(store.getResumeFilePath())
                        .status(ResumeConstant.PARSING_STATUS_PROCESSING)
                        .createdAt(store.getCreateTime() != null ? store.getCreateTime().toString() : null)
                        .build();
            }
            return ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .status(ResumeConstant.PARSING_STATUS_PROCESSING)
                    .build();
        }

        // 3. 构建并返回 VO
        return buildAnalysisResultVO(result, vectorStoreId);
    }

    @Override
    public List<ResumeAnalysisResultVO> getAnalysisList(Long userId, Long cursor, Integer limit) {
        List<ResumeAnalysisResult> results = resumeMapper.selectByUserId(userId, cursor, limit);
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        return results.stream()
                .map(r -> buildAnalysisResultVO(r, r.getVectorStoreId()))
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<byte[]> preview(String vectorStoreId, String disposition) {
        log.info("开始预览简历，vectorStoreId: {}, disposition: {}", vectorStoreId, disposition);

        // 1. 查询分析结果获取文件路径
        ResumeAnalysisResult result = resumeMapper.selectByVectorStoreId(vectorStoreId);
        if (result == null || result.getResumeFilePath() == null) {
            // 尝试从 user_vector_store 获取
            UserVectorStore store = userVectorStoreMapper.selectByVectorStoreId(vectorStoreId);
            if (store == null || store.getResumeFilePath() == null) {
                log.warn("简历文件不存在或已删除，vectorStoreId: {}", vectorStoreId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new byte[0]);
            }
            return buildPreviewResponse(store.getResumeFilePath(), disposition, null);
        }

        return buildPreviewResponse(result.getResumeFilePath(), disposition, result.getOriginalFileName());
    }

    @Override
    public String getPreviewUrl(String vectorStoreId) {
        log.info("获取简历预览 URL，vectorStoreId: {}", vectorStoreId);

        // 1. 查询分析结果获取文件路径
        ResumeAnalysisResult result = resumeMapper.selectByVectorStoreId(vectorStoreId);
        String fileUrl;
        if (result == null || result.getResumeFilePath() == null) {
            // 尝试从 user_vector_store 获取
            UserVectorStore store = userVectorStoreMapper.selectByVectorStoreId(vectorStoreId);
            if (store == null || store.getResumeFilePath() == null) {
                log.warn("简历文件不存在或已删除，vectorStoreId: {}", vectorStoreId);
                throw new FileNotFoundException("简历文件不存在或已删除");
            }
            fileUrl = store.getResumeFilePath();
        } else {
            fileUrl = result.getResumeFilePath();
        }

        // 2. 生成签名 URL（1 小时有效期）
        return aliOssUtil.generatePresignedUrl(fileUrl, 60);
    }

    @Override
    public CapabilityProfileVO getCapabilityProfile(Long userId) {
        StudentCapabilityProfile profile = capabilityProfileMapper.selectByUserId(userId);
        if (profile == null) {
            return null;
        }
        return buildCapabilityProfileVO(profile);
    }

    /**
     * 调用 AI 生成学生能力画像并保存到数据库
     * @param userId 用户 ID
     * @param analysisId 分析记录 ID
     * @param resumeContent 简历内容
     * @param targetJob 目标岗位（可为 null）
     */
    private void generateCapabilityProfile(Long userId, Long analysisId, String resumeContent, JobCategory targetJob) {
        log.info("开始生成学生能力画像，userId: {}, analysisId: {}, targetJob: {}", userId, analysisId,
                targetJob != null ? targetJob.getJobCategoryName() : "未匹配");

        String targetJobPrompt = "";
        if (targetJob != null) {
            targetJobPrompt = """

                    【目标岗位要求】
                    - 岗位名称：%s
                    - 岗位要求技能：%s
                    - 岗位描述：%s

                    请结合该目标岗位的需求，在7个维度的评分时以岗位要求为参考基准。
                    60分为及格线，表示学生已经初步具备进入该岗位并工作的能力。
                    如果学生具备岗位所需的核心技能，评分应适当提高；
                    如果缺少岗位关键技能，相关维度评分应低于60分。
                    最低评分为 30 分（例如简历上完全没有相关证书/技能，对应维度给 30 分）。
                    """.formatted(
                    targetJob.getJobCategoryName(),
                    targetJob.getRequiredSkills(),
                    targetJob.getJobDescription()
            );
        }

        // 1. 调用 AI 分析
        String prompt = """
                请分析以下简历内容，生成学生就业能力画像：

                ===== 简历开始 =====
                %s
                ===== 简历结束 =====
                %s

                评分范围说明：所有评分范围为 30-100 分，最低 30 分（表示完全没有相关内容），60 分为及格（初步具备岗位能力），100 分为优秀。
                请按照要求的 JSON 格式返回能力画像，直接返回 JSON 对象，不要包含任何 markdown 标记或额外说明。
                """.formatted(resumeContent, targetJobPrompt);

        ChatResponse response = resumeAnalysisChatClient.prompt()
                .system(SystemConstants.CAPABILITY_PROFILE_PROMPT)
                .user(prompt)
                .call()
                .chatResponse();

        String capabilityJson = response.getResult().getOutput().getText();
        log.info("AI 返回的能力画像 JSON：{}", capabilityJson);

        // 2. 解析 JSON
        String cleanedJson = cleanJsonResponse(capabilityJson);
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(cleanedJson);
        } catch (JsonProcessingException e) {
            throw new ResumeAnalysisException("能力画像 JSON 解析失败：" + e.getMessage(), e);
        }

        // 3. 构建实体
        try {
            StudentCapabilityProfile profile = StudentCapabilityProfile.builder()
                    .userId(userId)
                    .resumeAnalysisId(analysisId)
                    .overallScore(getIntValue(rootNode, "overall_score", 0))
                    .completenessScore(getIntValue(rootNode, "completeness_score", 0))
                    .competitivenessScore(getIntValue(rootNode, "competitiveness_score", 0))
                    .capabilityScores(objectMapper.writeValueAsString(rootNode.get("capability_scores")))
                    .professionalSkills(objectMapper.writeValueAsString(rootNode.get("professional_skills")))
                    .certificates(objectMapper.writeValueAsString(rootNode.get("certificates")))
                    .softSkills(objectMapper.writeValueAsString(rootNode.get("soft_skills")))
                    .aiEvaluation(getStringValue(rootNode, "ai_evaluation", ""))
                    .generatedAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 4. 插入数据库
            capabilityProfileMapper.insert(profile);
            log.info("学生能力画像已保存，profileId: {}, userId: {}", profile.getId(), userId);
        } catch (JsonProcessingException e) {
            throw new ResumeAnalysisException("序列化能力画像数据失败：" + e.getMessage(), e);
        }
    }

    /**
     * 生成用户职业数据（user_career_data），在简历分析完成后自动填充
     * @param userId 用户 ID
     * @param targetJob 目标岗位
     * @param targetJobName 目标岗位名称
     * @param targetJobId 目标岗位 ID
     * @param analysisRecord 简历分析记录
     */
    private void generateUserCareerData(Long userId, JobCategory targetJob, String targetJobName,
                                        Long targetJobId, ResumeAnalysisResult analysisRecord) {
        log.info("开始生成用户职业数据，userId: {}, targetJob: {}", userId, targetJobName);

        try {
            // 1. 构建 job_profile（岗位画像）
            String jobProfileJson = buildJobProfile(targetJob);

            // 2. 获取学生能力画像
            StudentCapabilityProfile profile = capabilityProfileMapper.selectByUserId(userId);

            // 3. 构建 match_summary（匹配度摘要）
            String matchSummaryJson = buildMatchSummary(profile, targetJob);

            // 4. 构建 skill_radar（能力雷达）
            String skillRadarJson = buildSkillRadar(profile);

            // 5. 构建 actions（行动建议，从 resume_analysis_result.suggestions 转换）
            String actionsJson = buildActions(analysisRecord.getSuggestions());

            // 6. 构建 market_trends（市场趋势，使用默认数据）
            String marketTrendsJson = buildDefaultMarketTrends(targetJobName);

            // 7. 检查是否已有记录
            UserCareerData existing = userCareerDataMapper.selectByUserId(userId);

            UserCareerData careerData = UserCareerData.builder()
                    .userId(userId)
                    .targetJob(targetJobName)
                    .targetJobId(targetJobId)
                    .jobProfile(jobProfileJson)
                    .matchSummary(matchSummaryJson)
                    .marketTrends(marketTrendsJson)
                    .skillRadar(skillRadarJson)
                    .actions(actionsJson)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            if (existing != null) {
                careerData.setId(existing.getId());
                userCareerDataMapper.update(careerData);
                log.info("用户职业数据已更新，userId: {}", userId);
            } else {
                userCareerDataMapper.insert(careerData);
                log.info("用户职业数据已创建，userId: {}", userId);
            }

            // 8. 同时生成 user_roadmap_steps（职业发展路径）
            generateUserRoadmapSteps(userId, targetJob, targetJobId);
        } catch (Exception e) {
            log.error("生成用户职业数据失败，userId: {}", userId, e);
            throw new RuntimeException("生成用户职业数据失败", e);
        }
    }

    /**
     * 构建岗位画像 JSON
     */
    private String buildJobProfile(JobCategory targetJob) {
        try {
            if (targetJob == null) {
                return "{}";
            }

            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", targetJob.getId());
            profile.put("name", targetJob.getJobCategoryName());
            profile.put("level", targetJob.getJobLevelName());
            profile.put("industry", "待定");
            profile.put("city", "待定");

            if (targetJob.getMinSalary() != null && targetJob.getMaxSalary() != null) {
                profile.put("salary_range_min", targetJob.getMinSalary());
                profile.put("salary_range_max", targetJob.getMaxSalary());
            }
            profile.put("description", targetJob.getJobDescription() != null ? targetJob.getJobDescription() : "");

            // 岗位所需技能
            List<Map<String, Object>> skills = new ArrayList<>();
            if (targetJob.getRequiredSkills() != null && !targetJob.getRequiredSkills().equals("[]")) {
                List<String> skillList = objectMapper.readValue(targetJob.getRequiredSkills(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                for (String skill : skillList) {
                    Map<String, Object> skillMap = new LinkedHashMap<>();
                    skillMap.put("name", skill);
                    skillMap.put("level", 3);
                    skillMap.put("category", "专业技能");
                    skills.add(skillMap);
                }
            }
            profile.put("skills", skills);

            // 能力权重（默认权重）
            Map<String, Double> weights = new LinkedHashMap<>();
            weights.put("professional_skill", 1.5);
            weights.put("certificate", 1.0);
            weights.put("innovation", 1.2);
            weights.put("learning", 1.2);
            weights.put("resilience", 1.0);
            weights.put("communication", 1.0);
            weights.put("internship", 1.3);
            profile.put("capability_weights", weights);

            return objectMapper.writeValueAsString(profile);
        } catch (Exception e) {
            log.error("构建岗位画像失败", e);
            return "{}";
        }
    }

    /**
     * 构建匹配度摘要 JSON
     */
    private String buildMatchSummary(StudentCapabilityProfile profile, JobCategory targetJob) {
        try {
            Map<String, Object> summary = new LinkedHashMap<>();

            if (profile == null) {
                summary.put("score", 0);
                summary.put("description", "暂无能力画像数据，请重新上传简历");
                summary.put("tags", new ArrayList<>());
                summary.put("dimension_scores", new LinkedHashMap<>());
                return objectMapper.writeValueAsString(summary);
            }

            // 计算匹配分数（简单平均或加权）
            int overallScore = profile.getOverallScore();
            summary.put("score", overallScore);

            // 描述
            String description;
            if (targetJob != null) {
                description = String.format("您与目标岗位【%s】的综合匹配度为 %d 分。",
                        targetJob.getJobCategoryName(), overallScore);
            } else {
                description = String.format("您的综合能力评分为 %d 分。", overallScore);
            }
            if (overallScore >= 80) {
                description += "表现优秀，已具备较强的岗位竞争力。";
            } else if (overallScore >= 60) {
                description += "初步具备进入岗位工作的能力，建议继续提升薄弱环节。";
            } else {
                description += "建议加强专业技能学习和实践经验的积累。";
            }
            summary.put("description", description);

            // 从简历亮点转换为标签
            List<String> tags = new ArrayList<>();
            if (profile.getAiEvaluation() != null && !profile.getAiEvaluation().isEmpty()) {
                tags.add("AI综合评价已生成");
            }
            summary.put("tags", tags);

            // 各维度得分
            Map<String, Object> dimensionScores = new LinkedHashMap<>();
            if (profile.getCapabilityScores() != null && !profile.getCapabilityScores().equals("{}")) {
                Map<String, Integer> scores = objectMapper.readValue(profile.getCapabilityScores(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
                dimensionScores.put("technical", scores.getOrDefault("professional_skill", 0));
                dimensionScores.put("innovation", scores.getOrDefault("innovation", 0));
                dimensionScores.put("resilience", scores.getOrDefault("resilience", 0));
                dimensionScores.put("communication", scores.getOrDefault("communication", 0));
                dimensionScores.put("learning", scores.getOrDefault("learning", 0));
                dimensionScores.put("internship", scores.getOrDefault("internship", 0));
            }
            summary.put("dimension_scores", dimensionScores);

            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            log.error("构建匹配度摘要失败", e);
            return "{\"score\":0,\"description\":\"数据生成失败\",\"tags\":[],\"dimension_scores\":{}}";
        }
    }

    /**
     * 构建能力雷达 JSON
     */
    private String buildSkillRadar(StudentCapabilityProfile profile) {
        try {
            Map<String, Object> radar = new LinkedHashMap<>();

            if (profile != null && profile.getCapabilityScores() != null
                    && !profile.getCapabilityScores().equals("{}")) {
                Map<String, Integer> scores = objectMapper.readValue(profile.getCapabilityScores(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class));
                radar.put("technical", scores.getOrDefault("professional_skill", 0));
                radar.put("innovation", scores.getOrDefault("innovation", 0));
                radar.put("resilience", scores.getOrDefault("resilience", 0));
                radar.put("communication", scores.getOrDefault("communication", 0));
                radar.put("learning", scores.getOrDefault("learning", 0));
                radar.put("internship", scores.getOrDefault("internship", 0));
            } else {
                radar.put("technical", 0);
                radar.put("innovation", 0);
                radar.put("resilience", 0);
                radar.put("communication", 0);
                radar.put("learning", 0);
                radar.put("internship", 0);
            }

            return objectMapper.writeValueAsString(radar);
        } catch (Exception e) {
            log.error("构建能力雷达失败", e);
            return "{\"technical\":0,\"innovation\":0,\"resilience\":0,\"communication\":0,\"learning\":0,\"internship\":0}";
        }
    }

    /**
     * 构建行动建议 JSON（从 resume_analysis_result.suggestions 转换）
     */
    private String buildActions(String suggestionsJson) {
        try {
            if (suggestionsJson == null || suggestionsJson.equals("[]")) {
                return "[]";
            }

            List<Map<String, Object>> suggestions = objectMapper.readValue(suggestionsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            List<Map<String, Object>> actions = new ArrayList<>();
            int index = 0;
            for (Map<String, Object> suggestion : suggestions) {
                Map<String, Object> action = new LinkedHashMap<>();
                action.put("id", "a_" + String.format("%03d", ++index));

                String type = (String) suggestion.getOrDefault("type", "CONTENT");
                String content = (String) suggestion.getOrDefault("content", "");

                action.put("title", content.length() > 30 ? content.substring(0, 30) + "..." : content);
                action.put("desc", content);

                // 根据类型设置图标和优先级
                if ("SKILL".equals(type)) {
                    action.put("icon", "school");
                    action.put("priority", "high");
                } else if ("LAYOUT".equals(type)) {
                    action.put("icon", "format");
                    action.put("priority", "low");
                } else {
                    action.put("icon", "info");
                    action.put("priority", "medium");
                }
                action.put("link", "/goals");

                actions.add(action);
            }

            return objectMapper.writeValueAsString(actions);
        } catch (Exception e) {
            log.error("构建行动建议失败", e);
            return "[]";
        }
    }

    /**
     * 构建默认市场趋势数据
     */
    private String buildDefaultMarketTrends(String targetJobName) {
        try {
            List<Map<String, Object>> trends = new ArrayList<>();
            if (targetJobName != null) {
                Map<String, Object> trend = new LinkedHashMap<>();
                trend.put("name", targetJobName);
                trend.put("growth", 0.10);
                trend.put("value", 75);
                trend.put("source", "AI 预测");
                trends.add(trend);
            }
            return objectMapper.writeValueAsString(trends);
        } catch (Exception e) {
            log.error("构建市场趋势失败", e);
            return "[]";
        }
    }

    /**
     * 生成用户职业发展路径（user_roadmap_steps）
     * 基于目标岗位的垂直晋升路径（同一岗位类别，不同级别）
     */
    private void generateUserRoadmapSteps(Long userId, JobCategory targetJob, Long targetJobId) {
        if (targetJob == null || targetJobId == null) {
            log.warn("目标岗位为空，跳过生成职业发展路径，userId: {}", userId);
            return;
        }

        log.info("开始生成用户职业发展路径，userId: {}, targetJob: {}", userId, targetJob.getJobCategoryName());

        try {
            // 1. 查询同一 job_category_code 基础类别的所有级别（垂直晋升路径）
            String baseCategoryCode = targetJob.getJobCategoryCode()
                    .replaceAll("_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "");
            List<JobCategory> verticalPath = jobCategoryMapper.selectVerticalPathByCategoryCode(baseCategoryCode);

            if (verticalPath == null || verticalPath.isEmpty()) {
                log.warn("未找到垂直晋升路径，baseCategoryCode: {}", baseCategoryCode);
                return;
            }

            // 2. 构建 steps 列表
            List<Map<String, Object>> steps = new ArrayList<>();
            int index = 0;
            for (JobCategory job : verticalPath) {
                Map<String, Object> step = new LinkedHashMap<>();
                step.put("job_id", job.getId());
                step.put("title", job.getJobCategoryName());
                step.put("level", job.getJobLevel());
                step.put("level_name", job.getJobLevelName());

                // 根据级别设置时间目标
                String timeGoal = switch (job.getJobLevel()) {
                    case "INTERNSHIP" -> "目标：第 0-1 年";
                    case "JUNIOR" -> "目标：第 1-3 年";
                    case "MID" -> "目标：第 3-5 年";
                    case "SENIOR" -> "目标：第 5-8 年";
                    default -> "目标：待定";
                };
                step.put("time", timeGoal);

                // 图标根据级别设置
                String icon = switch (job.getJobLevel()) {
                    case "INTERNSHIP" -> "person";
                    case "JUNIOR" -> "star";
                    case "MID" -> "diamond";
                    case "SENIOR" -> "flag";
                    default -> "circle";
                };
                step.put("icon", icon);

                // 匹配状态和 active 状态
                if (index == 0) {
                    step.put("status", "85% 匹配");
                    step.put("active", true);
                } else {
                    step.put("status", "待解锁");
                    step.put("active", false);
                }

                steps.add(step);
                index++;
            }

            String stepsJson = objectMapper.writeValueAsString(steps);

            // 3. 检查是否已有记录
            UserRoadmapSteps existing = userRoadmapStepsMapper.selectByUserId(userId);

            UserRoadmapSteps roadmapSteps = UserRoadmapSteps.builder()
                    .userId(userId)
                    .jobProfileId(targetJobId)
                    .currentStepIndex(0)
                    .steps(stepsJson)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            if (existing != null) {
                roadmapSteps.setId(existing.getId());
                userRoadmapStepsMapper.update(roadmapSteps);
                log.info("用户职业发展路径已更新，userId: {}", userId);
            } else {
                userRoadmapStepsMapper.insert(roadmapSteps);
                log.info("用户职业发展路径已创建，userId: {}, steps 数量: {}", userId, steps.size());
            }
        } catch (Exception e) {
            log.error("生成用户职业发展路径失败，userId: {}", userId, e);
        }
    }
    private int getIntValue(JsonNode rootNode, String fieldName, int defaultValue) {
        var node = rootNode.get(fieldName);
        if (node != null && node.isNumber()) {
            return node.asInt();
        }
        return defaultValue;
    }

    /**
     * 从 JsonNode 安全获取字符串值
     */
    private String getStringValue(JsonNode rootNode, String fieldName, String defaultValue) {
        var node = rootNode.get(fieldName);
        if (node != null && node.isTextual()) {
            return node.asText();
        }
        return defaultValue;
    }

    /**
     * 构建能力画像 VO
     */
    private CapabilityProfileVO buildCapabilityProfileVO(StudentCapabilityProfile profile) {
        try {
            CapabilityProfileVO.CapabilityProfileVOBuilder builder = CapabilityProfileVO.builder()
                    .id(profile.getId())
                    .userId(profile.getUserId())
                    .overallScore(profile.getOverallScore())
                    .completenessScore(profile.getCompletenessScore())
                    .competitivenessScore(profile.getCompetitivenessScore())
                    .aiEvaluation(profile.getAiEvaluation())
                    .generatedAt(profile.getGeneratedAt() != null ? profile.getGeneratedAt().toString() : null);

            // 解析 capability_scores
            if (profile.getCapabilityScores() != null && !profile.getCapabilityScores().isEmpty()
                    && !profile.getCapabilityScores().equals("{}")) {
                builder.capabilityScores(objectMapper.readValue(
                        profile.getCapabilityScores(),
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Integer.class)
                ));
            }

            // 解析 professional_skills
            if (profile.getProfessionalSkills() != null && !profile.getProfessionalSkills().isEmpty()
                    && !profile.getProfessionalSkills().equals("[]")) {
                builder.professionalSkills(objectMapper.readValue(
                        profile.getProfessionalSkills(),
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, CapabilityProfileVO.ProfessionalSkill.class
                        )
                ));
            }

            // 解析 certificates
            if (profile.getCertificates() != null && !profile.getCertificates().isEmpty()
                    && !profile.getCertificates().equals("[]")) {
                builder.certificates(objectMapper.readValue(
                        profile.getCertificates(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
                ));
            }

            // 解析 soft_skills
            if (profile.getSoftSkills() != null && !profile.getSoftSkills().isEmpty()
                    && !profile.getSoftSkills().equals("{}")) {
                builder.softSkills(objectMapper.readValue(
                        profile.getSoftSkills(),
                        objectMapper.getTypeFactory().constructMapType(
                                Map.class, String.class, CapabilityProfileVO.SoftSkillDetail.class
                        )
                ));
            }

            return builder.build();
        } catch (JsonProcessingException e) {
            log.error("解析能力画像 JSON 失败：{}", e.getMessage());
            return CapabilityProfileVO.builder()
                    .id(profile.getId())
                    .userId(profile.getUserId())
                    .overallScore(profile.getOverallScore())
                    .build();
        }
    }

    /**
     * 校验上传的文件
     */
    private void validateFile(MultipartFile file) {
        // 检查文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        // 检查文件大小（最大 10MB）
        if (file.getSize() > ResumeConstant.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持 " + (ResumeConstant.MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("无法获取文件名");
        }

        String fileType = getFileExtension(originalFilename);
        if (!ResumeConstant.ALLOWED_FILE_TYPES.contains(fileType.toLowerCase())) {
            throw new IllegalArgumentException("UNSUPPORTED_FILE_TYPE: 不支持的文件类型：" + fileType + "，请上传 PDF、DOCX、TXT 或 MD 格式文件");
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
     * 解析简历文件
     * @param fileBytes 文件字节数组
     * @param fileType 文件类型：pdf / docx / pptx / html / txt
     * @param userId 用户 ID
     * @param fileUrl 文件存储路径
     * @return 解析后的 Document 列表（metadata 中已包含用户 ID 和文件路径）
     */
    private List<Document> parseResume(byte[] fileBytes, String fileType, Long userId, String fileUrl) throws IOException {
        ByteArrayResource resource = new ByteArrayResource(fileBytes);
        String baseDocId = UUID.randomUUID().toString();

        // 构建基础 metadata
        Map<String, Object> baseMetadata = buildBaseMetadata(userId, fileUrl, fileType, baseDocId);
        log.debug("开始解析简历文件：userId={}, fileType={}, fileUrl={}, baseDocId={}",
                userId, fileType, fileUrl, baseDocId);

        if ("pdf".equals(fileType)) {
            return parseResumeByPdf(resource, fileBytes, baseMetadata, userId, fileUrl);
        } else {
            return parseResumeByTika(resource, baseMetadata, fileType);
        }
    }

    private List<Document> parseResumeByPdf(ByteArrayResource resource, byte[] fileBytes,
                                            Map<String, Object> baseMetadata, Long userId, String fileUrl) {
        log.debug("开始执行 PDF 文本提取：userId={}, fileUrl={}", userId, fileUrl);

        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                        .withPagesPerDocument(ResumeConstant.PDF_PAGES_PER_DOCUMENT)
                        .build()
        );
        List<Document> pageDocuments = reader.read();
        log.info("PDF 简历文本提取完成，共{}页文档", pageDocuments.size());

        int rawEffectiveChars = countEffectiveChars(joinDocumentTexts(pageDocuments));
        log.debug("PDF 文本提取统计：rawDocumentCount={}, rawEffectiveChars={}, minEffectiveChars={}, minEffectiveCharsPerPage={}",
                pageDocuments.size(), rawEffectiveChars, pdfMinEffectiveChars, pdfMinEffectiveCharsPerPage);

        if (isTextExtractionInsufficient(pageDocuments)) {
            log.warn("检测到疑似图片型 PDF 或文本过少，userId={}, fileUrl={}, rawEffectiveChars={}",
                    userId, fileUrl, rawEffectiveChars);
            pageDocuments = parsePdfWithOcrFallback(fileBytes, baseMetadata, userId, fileUrl, rawEffectiveChars);
        }

        validateExtractedResumeText(pageDocuments, "PDF_OCR_OR_TEXT");

        List<Document> splitDocuments = splitDocuments(pageDocuments);
        log.info("PDF 文本分割完成，共{}个片段", splitDocuments.size());

        for (int i = 0; i < splitDocuments.size(); i++) {
            splitDocuments.get(i).getMetadata().putAll(baseMetadata);
            splitDocuments.get(i).getMetadata().put(ResumeConstant.METADATA_KEY_CHUNK_NUMBER, i);
        }

        validateExtractedResumeText(splitDocuments, "PDF_SPLIT");
        return splitDocuments;
    }

    private List<Document> parseResumeByTika(ByteArrayResource resource, Map<String, Object> baseMetadata, String fileType) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.read();
        log.info("Tika 简历解析成功 ({}), 共{}个文档", fileType, documents.size());
        log.debug("Tika 文本提取统计：fileType={}, rawEffectiveChars={}", fileType, countEffectiveChars(joinDocumentTexts(documents)));

        List<Document> splitDocuments = splitDocuments(documents);
        log.info("文本分割完成，共{}个片段", splitDocuments.size());

        for (int i = 0; i < splitDocuments.size(); i++) {
            splitDocuments.get(i).getMetadata().putAll(baseMetadata);
            splitDocuments.get(i).getMetadata().put(ResumeConstant.METADATA_KEY_CHUNK_NUMBER, i);
        }

        validateExtractedResumeText(splitDocuments, "TIKA_SPLIT");
        return splitDocuments;
    }

    private List<Document> parsePdfWithOcrFallback(byte[] fileBytes, Map<String, Object> baseMetadata,
                                                   Long userId, String fileUrl, int rawEffectiveChars) {
        if (!pdfOcrEnabled) {
            log.warn("检测到疑似图片型 PDF，但 OCR 未启用，userId={}, fileUrl={}", userId, fileUrl);
            throw new FileUploadException("RESUME_IMAGE_PDF_OCR_REQUIRED: 该 PDF 可能为扫描件或图片，当前系统未启用 OCR，请上传可复制文本版 PDF 或 DOCX");
        }

        log.warn("开始对疑似图片型 PDF 执行 OCR fallback，userId={}, fileUrl={}, rawEffectiveChars={}",
                userId, fileUrl, rawEffectiveChars);

        List<Document> ocrDocuments = resumeOcrService.extractDocumentsFromPdf(fileBytes, baseMetadata);
        int ocrEffectiveChars = countEffectiveChars(joinDocumentTexts(ocrDocuments));
        log.debug("OCR fallback 完成：userId={}, fileUrl={}, ocrDocumentCount={}, ocrEffectiveChars={}",
                userId, fileUrl, ocrDocuments.size(), ocrEffectiveChars);

        if (isTextExtractionInsufficient(ocrDocuments)) {
            log.warn("OCR fallback 后文本仍不足，userId={}, fileUrl={}, ocrEffectiveChars={}", userId, fileUrl, ocrEffectiveChars);
            throw new FileUploadException("RESUME_OCR_FAILED: 简历内容识别失败，请上传可编辑文本版 PDF 或 DOCX");
        }

        return ocrDocuments;
    }

    private Map<String, Object> buildBaseMetadata(Long userId, String fileUrl, String fileType, String baseDocId) {
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put(ResumeConstant.METADATA_KEY_USER_ID, userId);
        baseMetadata.put(ResumeConstant.METADATA_KEY_FILE_PATH, fileUrl);
        baseMetadata.put(ResumeConstant.METADATA_KEY_FILE_TYPE, fileType);
        baseMetadata.put(ResumeConstant.METADATA_KEY_DOCUMENT_ID, baseDocId);
        return baseMetadata;
    }

    private List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ResumeConstant.TEXT_SPLITTER_CHUNK_SIZE)
                .withMinChunkSizeChars(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_SIZE_CHARS)
                .withMinChunkLengthToEmbed(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_LENGTH_TO_EMBED)
                .withMaxNumChunks(ResumeConstant.TEXT_SPLITTER_MAX_NUM_CHUNKS)
                .build();

        List<Document> splitDocuments = splitter.apply(documents);
        log.debug("文本分割统计：rawDocumentCount={}, splitDocumentCount={}, splitEffectiveChars={}",
                documents.size(), splitDocuments.size(), countEffectiveChars(joinDocumentTexts(splitDocuments)));
        return splitDocuments;
    }

    private void validateExtractedResumeText(List<Document> documents, String stage) {
        int effectiveChars = countEffectiveChars(joinDocumentTexts(documents));
        if (documents == null || documents.isEmpty()) {
            log.warn("简历解析阶段 {} 未生成任何文档", stage);
            throw new FileUploadException("RESUME_TEXT_EXTRACTION_FAILED: 简历未识别到有效内容，请上传可编辑文本版 PDF 或 DOCX");
        }
        if (effectiveChars == 0) {
            log.warn("简历解析阶段 {} 的有效字符数为 0", stage);
            throw new FileUploadException("RESUME_TEXT_EXTRACTION_FAILED: 简历内容为空，无法继续分析");
        }
        log.debug("简历解析阶段 {} 校验通过：documentCount={}, effectiveChars={}", stage, documents.size(), effectiveChars);
    }

    private boolean isTextExtractionInsufficient(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            log.warn("文本提取结果为空，判定为提取不足");
            return true;
        }

        String mergedText = joinDocumentTexts(documents);
        int totalEffectiveChars = countEffectiveChars(mergedText);
        int avgEffectiveCharsPerDocument = totalEffectiveChars / documents.size();

        boolean insufficient = totalEffectiveChars < pdfMinEffectiveChars
                || avgEffectiveCharsPerDocument < pdfMinEffectiveCharsPerPage
                || allDocumentsNearlyEmpty(documents);

        log.debug("文本有效性判定：documentCount={}, totalEffectiveChars={}, avgEffectiveCharsPerDocument={}, insufficient={}",
                documents.size(), totalEffectiveChars, avgEffectiveCharsPerDocument, insufficient);

        if (insufficient) {
            log.warn("文本提取结果不足：documentCount={}, totalEffectiveChars={}, avgEffectiveCharsPerDocument={}",
                    documents.size(), totalEffectiveChars, avgEffectiveCharsPerDocument);
        }
        return insufficient;
    }

    private boolean allDocumentsNearlyEmpty(List<Document> documents) {
        for (Document document : documents) {
            if (countEffectiveChars(document.getText()) >= Math.max(10, pdfMinEffectiveCharsPerPage)) {
                return false;
            }
        }
        return true;
    }

    private String joinDocumentTexts(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return "";
        }
        return documents.stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
    }

    private int countEffectiveChars(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
    }

    /**
     * 调用 AI 进行简历解析（结合目标岗位）
     */
    private String callAiForAnalysis(String resumeContent, JobCategory targetJob) {
        String targetJobInfo = "";
        if (targetJob != null) {
            // 获取晋升路径信息（下一级岗位）
            String promotionPathInfo = buildPromotionPathInfo(targetJob);

            targetJobInfo = """

                    【用户目标岗位信息】
                    - 岗位名称：%s
                    - 岗位级别：%s
                    - 岗位要求技能：%s
                    - 岗位描述：%s
                    %s

                    请结合该目标岗位的要求，在 suggestions 字段中给出针对性的优化建议。
                    建议编写要求：
                    1. 重点编写用户当前技能与目标岗位要求技能之间的差距，给出具体的学习建议
                    2. 建议应聚焦于通用技能的学习建议（如编程语言、框架、工具等），不要泛泛而谈
                    3. 用户已有的技能可以给出进阶学习建议（如从"会使用"到"深入理解原理"）
                    4. %s
                    5. 每条建议要具体、可执行，指出学什么、怎么学
                    """.formatted(
                    targetJob.getJobCategoryName(),
                    targetJob.getJobLevelName(),
                    targetJob.getRequiredSkills(),
                    targetJob.getJobDescription(),
                    promotionPathInfo,
                    promotionPathInfo.isEmpty()
                            ? "如果存在下一级晋升路径，可以加一两条晋升岗位需要的技能学习建议"
                            : "已提供下一级晋升路径信息，请结合该信息给出 1-2 条晋升技能学习建议"
            );
        }

        String userPrompt = """
            请分析以下简历内容：

            ===== 简历开始 =====
            %s
            ===== 简历结束 =====
            %s

            请按照要求的 JSON 格式返回分析结果，直接返回 JSON 对象，不要包含任何 markdown 标记或额外说明。
            """.formatted(resumeContent, targetJobInfo);

        ChatResponse response = resumeAnalysisChatClient.prompt()
                .system(SystemConstants.RESUME_ANALYSIS_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
    }

    /**
     * 构建晋升路径信息（同一岗位类别的下一级岗位）
     */
    private String buildPromotionPathInfo(JobCategory targetJob) {
        try {
            // 从 job_category_code 中去掉级别后缀，获取基础类别编码
            String baseCategoryCode = targetJob.getJobCategoryCode()
                    .replaceAll("_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "");

            // 查询该类别的所有级别岗位
            List<JobCategory> allLevels = jobCategoryMapper.selectVerticalPathByCategoryCode(baseCategoryCode);

            if (allLevels == null || allLevels.isEmpty()) {
                return "";
            }

            // 找到当前级别的索引，获取下一级
            List<String> levelOrder = List.of("INTERNSHIP", "JUNIOR", "MID", "SENIOR");
            int currentIndex = levelOrder.indexOf(targetJob.getJobLevel());

            if (currentIndex >= 0 && currentIndex < levelOrder.size() - 1) {
                String nextLevel = levelOrder.get(currentIndex + 1);
                // 找到下一级岗位
                for (JobCategory job : allLevels) {
                    if (nextLevel.equals(job.getJobLevel())) {
                        return String.format("""

                                【下一级晋升路径信息】
                                - 岗位名称：%s
                                - 岗位级别：%s
                                - 要求技能：%s
                                """,
                                job.getJobCategoryName(),
                                job.getJobLevelName(),
                                job.getRequiredSkills());
                    }
                }
            }

            return "";
        } catch (Exception e) {
            log.warn("获取晋升路径信息失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 匹配用户目标岗位
     * 1. 先从简历中提取用户技能
     * 2. 如果 job 表中存在该岗位，根据技能匹配度选择最接近的级别
     * 3. 如果不存在，将简历向量化后与 job_vector_store 比较，找相似度最高的岗位
     */
    private JobCategory matchTargetJob(String vectorStoreId, String resumeContent) {
        try {
            // 先从简历中提取用户技能
            List<String> userSkills = extractSkillsFromResume(resumeContent);
            log.info("从简历中提取到 {} 个技能: {}", userSkills.size(), userSkills);

            // 先尝试从 AI 解析的简历中提取 target_role
            String parsedRole = extractTargetRoleFromResume(resumeContent);

            // 策略 1：如果 job 表中存在用户目标岗位，根据技能匹配度选择级别
            if (parsedRole != null && !parsedRole.isEmpty() && !"null".equals(parsedRole)) {
                log.info("从简历中提取到目标岗位: {}，尝试在 job 表中查找", parsedRole);
                List<JobCategory> matchedJobs = jobCategoryMapper.searchByKeyword(parsedRole, 10);
                if (matchedJobs != null && !matchedJobs.isEmpty()) {
                    // 根据技能匹配度选择最接近的级别
                    JobCategory targetJob = selectBestSkillMatchJob(matchedJobs, userSkills);
                    log.info("在 job 表中找到匹配岗位: {} (id={}, level={})",
                            targetJob.getJobCategoryName(), targetJob.getId(), targetJob.getJobLevel());
                    return targetJob;
                }
            }

            // 策略 2：job 表不存在，使用向量搜索匹配最相似岗位
            log.info("job 表中未找到匹配岗位，开始向量相似度搜索");

            // 获取简历的 embedding 向量
            UserVectorStore userVector = userVectorStoreMapper.selectByVectorStoreId(vectorStoreId);
            if (resumeContent != null && !resumeContent.isBlank()) {
                String resumeVector = buildJobSearchQuery(parsedRole, userSkills, resumeContent);
                List<JobCategory> similarJobs = jobVectorSearchService.searchSimilarJobs(resumeVector, 5);

                if (similarJobs != null && !similarJobs.isEmpty()) {
                    // 根据技能匹配度选择最接近的级别
                    JobCategory bestMatch = selectBestSkillMatchJob(similarJobs, userSkills);
                    log.info("向量搜索找到最相似岗位: {} (id={}, level={})",
                            bestMatch.getJobCategoryName(), bestMatch.getId(), bestMatch.getJobLevel());
                    return bestMatch;
                }
            }

            log.warn("未能匹配到目标岗位");
            return null;

        } catch (Exception e) {
            log.error("匹配目标岗位失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从简历中提取技能列表（通过 AI）
     */
    private String buildJobSearchQuery(String parsedRole, List<String> userSkills, String resumeContent) {
        List<String> parts = new ArrayList<>();
        if (parsedRole != null && !parsedRole.isBlank() && !"null".equalsIgnoreCase(parsedRole.trim())) {
            parts.add("目标岗位：" + parsedRole.trim());
        }
        if (userSkills != null && !userSkills.isEmpty()) {
            List<String> limitedSkills = userSkills.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(skill -> !skill.isBlank())
                    .limit(10)
                    .toList();
            if (!limitedSkills.isEmpty()) {
                parts.add("技能：" + String.join("、", limitedSkills));
            }
        }
        if (parts.isEmpty() && resumeContent != null && !resumeContent.isBlank()) {
            parts.add(resumeContent.length() > 500 ? resumeContent.substring(0, 500) : resumeContent);
        }
        return String.join("\n", parts);
    }

    private List<String> extractSkillsFromResume(String resumeContent) {
        try {
            String prompt = """
                    请从以下简历内容中提取候选人的技能列表。
                    只返回技能名称，每行一个。如果找不到技能信息，返回 "none"。

                    ===== 简历开始 =====
                    %s
                    ===== 简历结束 =====
                    """.formatted(resumeContent);

            ChatResponse response = resumeAnalysisChatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            String result = response.getResult().getOutput().getText().trim();
            if ("none".equals(result) || result.isEmpty()) {
                return Collections.emptyList();
            }

            return Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !"none".equals(s.toLowerCase()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("提取技能失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 从多个同名岗位中，根据 required_skills 与用户技能的匹配度选择最接近的级别
     * 匹配度计算：用户技能与岗位 required_skills 的交集数量 / 岗位要求技能数量
     */
    private JobCategory selectBestSkillMatchJob(List<JobCategory> jobs, List<String> userSkills) {
        if (jobs.isEmpty()) {
            return null;
        }
        if (jobs.size() == 1 || userSkills.isEmpty()) {
            return jobs.get(0);
        }

        JobCategory bestJob = null;
        double bestMatchScore = -1;

        for (JobCategory job : jobs) {
            if (job.getRequiredSkills() == null || job.getRequiredSkills().equals("[]")) {
                continue;
            }
            try {
                List<String> requiredSkills = objectMapper.readValue(job.getRequiredSkills(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

                // 计算匹配度：交集 / 要求技能数
                long matchCount = requiredSkills.stream()
                        .filter(req -> userSkills.stream()
                                .anyMatch(user -> user.toLowerCase().contains(req.toLowerCase())
                                        || req.toLowerCase().contains(user.toLowerCase())))
                        .count();

                double matchScore = (double) matchCount / requiredSkills.size();
                log.debug("岗位 {} (level={}) 技能匹配度: {}/{} = {}",
                        job.getJobCategoryName(), job.getJobLevel(), matchCount, requiredSkills.size(), matchScore);

                if (matchScore > bestMatchScore) {
                    bestMatchScore = matchScore;
                    bestJob = job;
                }
            } catch (Exception e) {
                log.warn("解析岗位技能失败: {}", job.getJobCategoryName(), e);
            }
        }

        return bestJob != null ? bestJob : jobs.get(0);
    }

    /**
     * 从简历内容中提取目标岗位名称（通过 AI 快速解析）
     */
    private String extractTargetRoleFromResume(String resumeContent) {
        try {
            String prompt = """
                    请从以下简历内容中提取候选人的求职意向/目标岗位名称。
                    只返回岗位名称，不要返回其他任何内容。如果找不到目标岗位信息，返回 "null"。

                    ===== 简历开始 =====
                    %s
                    ===== 简历结束 =====
                    """.formatted(resumeContent);

            ChatResponse response = resumeAnalysisChatClient.prompt()
                    .user(prompt)
                    .call()
                    .chatResponse();

            String result = response.getResult().getOutput().getText().trim();
            if ("null".equals(result) || result.isEmpty()) {
                return null;
            }
            return result;

        } catch (Exception e) {
            log.warn("提取目标岗位失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 AI 返回的 JSON 结果
     * 处理可能的 markdown 代码块包装和字段缺失情况
     */
    private JsonAnalysisResult parseAnalysisJson(String json) throws JsonProcessingException {
        JsonAnalysisResult result = new JsonAnalysisResult();

        // 清理可能存在的 markdown 代码块标记
        String cleanedJson = cleanJsonResponse(json);
        log.debug("清理后的 JSON: {}", cleanedJson);

        // 使用 ObjectMapper 解析 JSON
        var rootNode = objectMapper.readTree(cleanedJson);

        // 提取 parsed_data 并转为 JSON 字符串（缺失时返回空对象）
        var parsedDataNode = rootNode.get("parsed_data");
        result.parsedDataJson = parsedDataNode != null && !parsedDataNode.isMissingNode()
                ? objectMapper.writeValueAsString(parsedDataNode)
                : "{}";

        // 提取 scores 并转为 JSON 字符串（缺失时返回默认评分）
        var scoresNode = rootNode.get("scores");
        if (scoresNode != null && !scoresNode.isMissingNode()) {
            result.scoresJson = objectMapper.writeValueAsString(scoresNode);
        } else {
            // 返回默认评分
            result.scoresJson = "{\"keyword_match\":0,\"layout\":0,\"skill_depth\":0,\"experience\":0}";
        }

        // 提取 highlights 并转为 JSON 字符串（缺失时返回空数组）
        var highlightsNode = rootNode.get("highlights");
        result.highlightsJson = highlightsNode != null && !highlightsNode.isMissingNode()
                ? objectMapper.writeValueAsString(highlightsNode)
                : "[]";

        // 提取 suggestions 并转为 JSON 字符串（缺失时返回空数组）
        var suggestionsNode = rootNode.get("suggestions");
        result.suggestionsJson = suggestionsNode != null && !suggestionsNode.isMissingNode()
                ? objectMapper.writeValueAsString(suggestionsNode)
                : "[]";

        return result;
    }

    /**
     * 清理 AI 返回的 JSON 响应，移除 markdown 代码块标记
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
     * 构建分析结果 VO
     */
    private ResumeAnalysisResultVO buildAnalysisResultVO(ResumeAnalysisResult result, String vectorStoreId) {
        try {
            ResumeAnalysisResultVO.ResumeAnalysisResultVOBuilder builder = ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .analysisId(result.getId())
                    .userId(result.getUserId())
                    .fileType(result.getFileType())
                    .originalFileName(result.getOriginalFileName())
                    .resumeFilePath(result.getResumeFilePath())
                    .status(result.getStatus())
                    .progress(result.getProgress())
                    .createdAt(result.getCreateTime() != null ? result.getCreateTime().toString() : null)
                    .updatedAt(result.getUpdateTime() != null ? result.getUpdateTime().toString() : null);

            // 解析 parsed_data
            if (result.getParsedData() != null && !result.getParsedData().isEmpty()
                    && !"{}".equals(result.getParsedData())) {
                builder.parsedData(objectMapper.readValue(result.getParsedData(), ResumeParsedData.class));
            }

            // 解析 scores
            if (result.getScores() != null && !result.getScores().isEmpty()
                    && !"{\"keyword_match\":0,\"layout\":0,\"skill_depth\":0,\"experience\":0}".equals(result.getScores())) {
                builder.scores(objectMapper.readValue(result.getScores(), ResumeScores.class));
            }

            // 解析 highlights
            if (result.getHighlights() != null && !result.getHighlights().isEmpty()
                    && !"[]".equals(result.getHighlights())) {
                builder.highlights(objectMapper.readValue(result.getHighlights(), List.class));
            }

            // 解析 suggestions
            if (result.getSuggestions() != null && !result.getSuggestions().isEmpty()
                    && !"[]".equals(result.getSuggestions())) {
                var suggestions = objectMapper.readValue(
                        result.getSuggestions(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, ResumeSuggestion.class)
                );
                builder.suggestions((List<ResumeSuggestion>) suggestions);
            }

            return builder.build();

        } catch (JsonProcessingException e) {
            log.error("解析分析结果 JSON 失败：{}", e.getMessage());
            return ResumeAnalysisResultVO.builder()
                    .vectorStoreId(vectorStoreId)
                    .analysisId(result.getId())
                    .status(result.getStatus())
                    .build();
        }
    }

    /**
     * 构建预览响应
     */
    private ResponseEntity<byte[]> buildPreviewResponse(String fileUrl, String disposition, String originalFileName) {
        try {
            // 1. 从 URL 中提取文件扩展名
            String fileType = extractFileType(fileUrl);
            String mimeType = MIME_TYPE_MAP.getOrDefault(fileType, "application/octet-stream");

            // 2. 检查是否支持内联预览
            if (!"attachment".equals(disposition) && !INLINE_PREVIEW_TYPES.contains(fileType.toLowerCase())) {
                log.warn("不支持预览该文件类型：{}", fileType);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(new byte[0]);
            }

            // 3. 从 OSS 下载文件
            byte[] fileBytes = aliOssUtil.download(fileUrl);
            log.info("文件下载成功，大小：{} bytes", fileBytes.length);

            // 4. 构建响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(mimeType));
            headers.setCacheControl("private, max-age=300"); // 5 分钟缓存
            headers.set("X-Content-Type-Options", "nosniff");

            // 5. 设置 Content-Disposition
            String disValue = "attachment".equals(disposition) ? "attachment" : "inline";
            String fileName = originalFileName != null ? originalFileName : extractFileName(fileUrl);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");
            headers.setContentDispositionFormData(disValue, encodedFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("简历预览失败，fileUrl: {}", fileUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * 从 URL 中提取文件扩展名
     */
    private String extractFileType(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "pdf";
        }
        int lastDot = fileUrl.lastIndexOf(".");
        if (lastDot > 0 && lastDot < fileUrl.length() - 1) {
            return fileUrl.substring(lastDot + 1).toLowerCase();
        }
        return "pdf";
    }

    /**
     * 从 URL 中提取文件名
     */
    private String extractFileName(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "resume.pdf";
        }
        int lastSlash = fileUrl.lastIndexOf("/");
        if (lastSlash > 0 && lastSlash < fileUrl.length() - 1) {
            return fileUrl.substring(lastSlash + 1);
        }
        return "resume.pdf";
    }

    /**
     * 内部类：用于临时存储解析后的 JSON 字符串
     */
    private static class JsonAnalysisResult {
        String parsedDataJson;
        String scoresJson;
        String highlightsJson;
        String suggestionsJson;
    }

    /**
     * 内部类：进度追踪器，每 5 秒更新一次进度
     */
    private class ProgressTracker {
        private volatile boolean running = true;
        private String vectorStoreId;
        private Thread trackThread;
        private final Random random = new Random();

        public void startTracking(String vectorStoreId) {
            this.vectorStoreId = vectorStoreId;
            trackThread = new Thread(() -> {
                int progress = 0;
                int updateCount = 0;
                while (running && progress < 100) {
                    try {
                        // 随机休眠 2-4 秒
                        Thread.sleep(2000 + random.nextInt(2000));
                        updateCount++;
                        // 动态增量：10 次轮询 (约 20s) 达到 65%，之后逐步增长到 90%+
                        // 前 10 次：平均 6.5%/次，累计约 65%
                        // 11-15 次：平均 5%/次，累计约 90%
                        // 之后：缓慢增长
                        int increment;
                        if (updateCount <= 10) {
                            // 前 10 次：8-11%，平均 9.5%，10 次累计约 95%，实际受随机值影响
                            increment = 8 + random.nextInt(4);
                        } else if (updateCount <= 15) {
                            // 第 11-15 次：6-9%，平均 7.5%
                            increment = 6 + random.nextInt(4);
                        } else {
                            // 之后：每次 4-6%，缓慢逼近上限
                            increment = 4 + random.nextInt(3);
                        }
                        progress = Math.min(progress + increment, 95);
                        ResumeAnalysisResult progressRecord = ResumeAnalysisResult.builder()
                                .vectorStoreId(vectorStoreId)
                                .status("processing")
                                .progress(progress)
                                .build();
                        resumeMapper.update(progressRecord);
                        log.debug("更新分析进度：vectorStoreId={}, progress={}, updateCount={}", vectorStoreId, progress, updateCount);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            trackThread.setDaemon(true);
            trackThread.start();
        }

        public void stopTracking() {
            running = false;
            if (trackThread != null) {
                trackThread.interrupt();
            }
        }
    }
}
