package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.itsheng.pojo.entity.ResumeAnalysisResult;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.pojo.vo.ResumeAnalysisResultVO;
import com.itsheng.pojo.vo.ResumeUploadVO;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.ResumeMapper;
import com.itsheng.service.mapper.UserVectorStoreMapper;
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
    private final AliOssUtil aliOssUtil;
    private final ChatClient resumeAnalysisChatClient;
    private final ObjectMapper objectMapper;

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
    }

    // 支持内联预览的文件类型
    private static final Set<String> INLINE_PREVIEW_TYPES = Set.of("pdf", "html", "htm", "txt");

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

            // 将简历内容添加到向量存储（每页作为一个独立的 Document）
            pgVectorStore.add(documents);
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

            // 3. 调用 AI 进行简历解析
            String analysisJson = callAiForAnalysis(resumeContent);
            log.info("AI 返回的解析结果：{}", analysisJson);

            // 4. 停止进度更新
            tracker.stopTracking();

            // 5. 解析 JSON 并提取各字段
            JsonAnalysisResult analysisResult = parseAnalysisJson(analysisJson);

            // 6. 更新分析结果记录为完成状态
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

            // 7. 更新数据库
            resumeMapper.update(analysisRecord);
            log.info("简历分析结果已更新，analysisId: {}", analysisRecord.getId());

            // 8. 更新 user_vector_store 表的状态为 COMPLETED
            UserVectorStore userVectorStore = UserVectorStore.builder()
                    .id(vectorStoreId)
                    .resumeContent(resumeContent)
                    .build();
            userVectorStoreMapper.update(userVectorStore);
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
            throw new IllegalArgumentException("UNSUPPORTED_FILE_TYPE: 不支持的文件类型：" + fileType + "，请上传 PDF、DOCX、PPTX、HTML 或 TXT 格式文件");
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
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put(ResumeConstant.METADATA_KEY_USER_ID, userId);
        baseMetadata.put(ResumeConstant.METADATA_KEY_FILE_PATH, fileUrl);
        baseMetadata.put(ResumeConstant.METADATA_KEY_FILE_TYPE, fileType);
        baseMetadata.put(ResumeConstant.METADATA_KEY_DOCUMENT_ID, baseDocId);

        if ("pdf".equals(fileType)) {
            // PDF 文件解析 - 先按页读取，然后使用 TokenTextSplitter 进行带重叠的分割
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .withPagesPerDocument(ResumeConstant.PDF_PAGES_PER_DOCUMENT)
                            .build()
            );
            List<Document> documents = reader.read();
            log.info("PDF 简历解析成功，共{}页", documents.size());

            // 使用 TokenTextSplitter 进行带重叠的分割，避免跨页内容丢失
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(ResumeConstant.TEXT_SPLITTER_CHUNK_SIZE)
                    .withMinChunkSizeChars(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_SIZE_CHARS)
                    .withMinChunkLengthToEmbed(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_LENGTH_TO_EMBED)
                    .withMaxNumChunks(ResumeConstant.TEXT_SPLITTER_MAX_NUM_CHUNKS)
                    .build();

            List<Document> splitDocuments = splitter.apply(documents);
            log.info("PDF 文本分割完成（带重叠），共{}个片段", splitDocuments.size());

            // 为每个 document 添加基础 metadata
            for (int i = 0; i < splitDocuments.size(); i++) {
                splitDocuments.get(i).getMetadata().putAll(baseMetadata);
                splitDocuments.get(i).getMetadata().put(ResumeConstant.METADATA_KEY_PAGE_NUMBER, i + 1);
            }
            return splitDocuments;
        } else {
            // 使用 Tika 处理 DOCX、PPTX、HTML、TXT 等格式
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.read();
            log.info("Tika 简历解析成功 ({}), 共{}个文档", fileType, documents.size());

            // 对于非 PDF 格式，使用文本分割器进行分割以适配向量模型
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(ResumeConstant.TEXT_SPLITTER_CHUNK_SIZE)
                    .withMinChunkSizeChars(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_SIZE_CHARS)
                    .withMinChunkLengthToEmbed(ResumeConstant.TEXT_SPLITTER_MIN_CHUNK_LENGTH_TO_EMBED)
                    .withMaxNumChunks(ResumeConstant.TEXT_SPLITTER_MAX_NUM_CHUNKS)
                    .build();

            List<Document> splitDocuments = splitter.apply(documents);
            log.info("文本分割完成，共{}个片段", splitDocuments.size());

            // 为每个 document 添加基础 metadata
            for (int i = 0; i < splitDocuments.size(); i++) {
                splitDocuments.get(i).getMetadata().putAll(baseMetadata);
                splitDocuments.get(i).getMetadata().put(ResumeConstant.METADATA_KEY_CHUNK_NUMBER, i);
            }
            return splitDocuments;
        }
    }

    /**
     * 调用 AI 进行简历解析
     */
    private String callAiForAnalysis(String resumeContent) {
        String userPrompt = """
            请分析以下简历内容：

            ===== 简历开始 =====
            %s
            ===== 简历结束 =====

            请按照要求的 JSON 格式返回分析结果，直接返回 JSON 对象，不要包含任何 markdown 标记或额外说明。
            """.formatted(resumeContent);

        ChatResponse response = resumeAnalysisChatClient.prompt()
                .system(SystemConstants.RESUME_ANALYSIS_PROMPT)
                .user(userPrompt)
                .call()
                .chatResponse();

        return response.getResult().getOutput().getText();
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
