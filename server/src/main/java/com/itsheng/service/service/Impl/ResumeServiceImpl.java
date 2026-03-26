package com.itsheng.service.service.Impl;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.entity.UserVectorStore;
import com.itsheng.pojo.vo.ResumeUploadVO;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.UserVectorStoreMapper;
import com.itsheng.service.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final VectorStore pgVectorStore;
    private final OpenAiEmbeddingModel embeddingModel;
    private final CommonController commonController;
    private final UserVectorStoreMapper userVectorStoreMapper;

    private static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList("pdf", "docx", "pptx", "html", "txt"));
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
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
                throw new RuntimeException("文件上传到 OSS 失败");
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

            // 返回 UploadVO
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ResumeUploadVO.builder()
                    .id(id)
                    .userId(String.valueOf(userId))
                    .resumeFilePath(fileUrl)
                    .parsingStatus("PROCESSING")
                    .createdAt(now)
                    .build();

        } catch (Exception e) {
            log.error("简历上传解析失败：{}", e.getMessage(), e);
            throw new RuntimeException("简历上传解析失败：" + e.getMessage(), e);
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
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制，最大支持 10MB");
        }

        // 获取文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("无法获取文件名");
        }

        String fileType = getFileExtension(originalFilename);
        if (!ALLOWED_FILE_TYPES.contains(fileType.toLowerCase())) {
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
        baseMetadata.put("user_id", userId);
        baseMetadata.put("file_path", fileUrl);
        baseMetadata.put("file_type", fileType);
        baseMetadata.put("document_id", baseDocId);

        if ("pdf".equals(fileType)) {
            // PDF 文件解析 - 每页作为一个 Document
            PagePdfDocumentReader reader = new PagePdfDocumentReader(
                    resource,
                    PdfDocumentReaderConfig.builder()
                            .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults())
                            .withPagesPerDocument(1) // 每 1 页 PDF 作为一个 Document
                            .build()
            );
            List<Document> documents = reader.read();
            log.info("PDF 简历解析成功，共{}页", documents.size());

            // 为每个 document 添加基础 metadata
            for (int i = 0; i < documents.size(); i++) {
                documents.get(i).getMetadata().putAll(baseMetadata);
                documents.get(i).getMetadata().put("page_number", i + 1);
            }
            return documents;
        } else {
            // 使用 Tika 处理 DOCX、PPTX、HTML、TXT 等格式
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.read();
            log.info("Tika 简历解析成功 ({}), 共{}个文档", fileType, documents.size());

            // 对于非 PDF 格式，使用文本分割器进行分割以适配向量模型
            TokenTextSplitter splitter = TokenTextSplitter.builder()
                    .withChunkSize(800)
                    .withMinChunkSizeChars(350)
                    .withMinChunkLengthToEmbed(10)
                    .withMaxNumChunks(5000)
                    .build();

            List<Document> splitDocuments = splitter.apply(documents);
            log.info("文本分割完成，共{}个片段", splitDocuments.size());

            // 为每个 document 添加基础 metadata
            for (int i = 0; i < splitDocuments.size(); i++) {
                splitDocuments.get(i).getMetadata().putAll(baseMetadata);
                splitDocuments.get(i).getMetadata().put("chunk_number", i);
            }
            return splitDocuments;
        }
    }
}
