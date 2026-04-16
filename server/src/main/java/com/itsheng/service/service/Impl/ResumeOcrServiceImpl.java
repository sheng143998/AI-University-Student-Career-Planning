package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.constant.ResumeConstant;
import com.itsheng.common.exception.ResumeAnalysisException;
import com.itsheng.service.service.ResumeOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeOcrServiceImpl implements ResumeOcrService {

    private static final String OCR_INSTRUCTION = """
            请识别这页简历图片中的全部文字内容。
            输出要求：
            1. 只返回识别后的纯文本
            2. 保留原有的分段、列表和换行结构
            3. 不要添加解释、总结、markdown 标记或额外说明
            4. 如果页面几乎没有可识别文本，返回空字符串
            """;

    private final ObjectMapper objectMapper;

    @Value("${spring.ai.openai.base-url}")
    private String openAiBaseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;

    @Value("${resume.parser.pdf.ocr-model:qwen-vl-ocr-2025-11-20}")
    private String ocrModel;

    @Value("${resume.parser.pdf.ocr-max-pages:10}")
    private int ocrMaxPages;

    @Value("${resume.parser.pdf.ocr-render-dpi:180}")
    private float ocrRenderDpi;

    @Value("${resume.parser.pdf.ocr-timeout-seconds:60}")
    private long ocrTimeoutSeconds;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Override
    public List<Document> extractDocumentsFromPdf(byte[] fileBytes, Map<String, Object> baseMetadata) {
        log.debug("开始执行 PDF OCR，模型={}, 文件大小={} bytes", ocrModel, fileBytes.length);

        try (PDDocument pdfDocument = Loader.loadPDF(fileBytes)) {
            int pageCount = pdfDocument.getNumberOfPages();
            int pagesToProcess = Math.min(pageCount, ocrMaxPages);

            if (pageCount > ocrMaxPages) {
                log.warn("PDF 页数 {} 超过 OCR 最大处理页数 {}，本次仅处理前 {} 页", pageCount, ocrMaxPages, pagesToProcess);
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            List<Document> documents = new ArrayList<>();
            int totalEffectiveChars = 0;

            for (int i = 0; i < pagesToProcess; i++) {
                int pageNumber = i + 1;
                log.debug("开始 OCR 第 {} 页，dpi={}", pageNumber, ocrRenderDpi);

                String pageText = requestOcrText(renderPageToDataUrl(renderer, i), pageNumber);
                int effectiveChars = countEffectiveChars(pageText);
                totalEffectiveChars += effectiveChars;

                if (effectiveChars == 0) {
                    log.warn("OCR 第 {} 页未识别到有效文本", pageNumber);
                } else {
                    log.debug("OCR 第 {} 页完成，有效字符数={}", pageNumber, effectiveChars);
                }

                Map<String, Object> metadata = new HashMap<>(baseMetadata);
                metadata.put(ResumeConstant.METADATA_KEY_PAGE_NUMBER, pageNumber);
                metadata.put("ocr_enabled", true);
                metadata.put("ocr_model", ocrModel);

                documents.add(Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text(pageText == null ? "" : pageText)
                        .metadata(metadata)
                        .build());
            }

            log.debug("PDF OCR 完成，处理页数={}, 累计有效字符数={}", documents.size(), totalEffectiveChars);
            return documents;
        } catch (IOException e) {
            log.warn("PDF 渲染为图片失败，无法执行 OCR: {}", e.getMessage(), e);
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: PDF 页面渲染失败，无法执行 OCR", e);
        }
    }

    private String renderPageToDataUrl(PDFRenderer renderer, int pageIndex) throws IOException {
        BufferedImage image = renderer.renderImageWithDPI(pageIndex, ocrRenderDpi, ImageType.RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            return "data:image/png;base64," + base64;
        }
    }

    private String requestOcrText(String imageDataUrl, int pageNumber) {
        try {
            String requestBody = buildRequestBody(imageDataUrl);
            String endpoint = normalizeBaseUrl(openAiBaseUrl) + "/chat/completions";

            log.debug("调用 OCR 模型识别第 {} 页，endpoint={}, model={}", pageNumber, endpoint, ocrModel);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + openAiApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(ocrTimeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("OCR 模型调用失败，第 {} 页返回状态码={}, body={}", pageNumber, response.statusCode(), response.body());
                throw new ResumeAnalysisException("RESUME_OCR_FAILED: OCR 模型调用失败，状态码=" + response.statusCode());
            }

            String text = extractTextFromResponse(response.body());
            log.debug("OCR 模型第 {} 页返回成功，原始响应长度={}", pageNumber, response.body().length());
            return text;
        } catch (ResumeAnalysisException e) {
            throw e;
        } catch (Exception e) {
            log.warn("OCR 模型识别第 {} 页失败: {}", pageNumber, e.getMessage(), e);
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: OCR 识别失败", e);
        }
    }

    private String buildRequestBody(String imageDataUrl) throws IOException {
        Map<String, Object> root = new HashMap<>();
        root.put("model", ocrModel);
        root.put("temperature", 0);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", OCR_INSTRUCTION);

        Map<String, Object> imageUrl = new HashMap<>();
        imageUrl.put("url", imageDataUrl);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        imagePart.put("image_url", imageUrl);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", List.of(textPart, imagePart));

        root.put("messages", List.of(message));
        return objectMapper.writeValueAsString(root);
    }

    private String extractTextFromResponse(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.warn("OCR 响应中缺少 choices 字段");
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: OCR 响应格式异常");
        }

        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return cleanupOcrText(contentNode.asText());
        }

        if (contentNode.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode item : contentNode) {
                if ("text".equals(item.path("type").asText()) && item.path("text").isTextual()) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(item.path("text").asText());
                }
            }
            return cleanupOcrText(builder.toString());
        }

        log.warn("OCR 响应中的 message.content 既不是字符串也不是数组");
        throw new ResumeAnalysisException("RESUME_OCR_FAILED: OCR 响应内容为空");
    }

    private String cleanupOcrText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            int firstLineBreak = cleaned.indexOf('\n');
            cleaned = firstLineBreak >= 0 ? cleaned.substring(firstLineBreak + 1) : cleaned;
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }
        return cleaned;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }

    private int countEffectiveChars(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
    }
}
