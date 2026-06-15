package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.itsheng.common.constant.ResumeConstant;
import com.itsheng.common.exception.ResumeAnalysisException;
import com.itsheng.service.client.PythonResumeAiClient;
import com.itsheng.service.model.ResumeDocument;
import com.itsheng.service.service.ResumeOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResumeOcrServiceImpl implements ResumeOcrService {

    private static final String OCR_INSTRUCTION = "Extract all readable resume text from this image. "
            + "Preserve paragraphs, lists, and line breaks. Return plain text only.";

    private final PythonResumeAiClient pythonResumeAiClient;

    @Value("${resume.parser.pdf.ocr-model:qwen-vl-ocr-2025-11-20}")
    private String ocrModel;

    @Value("${resume.parser.pdf.ocr-max-pages:10}")
    private int ocrMaxPages;

    @Value("${resume.parser.pdf.ocr-render-dpi:180}")
    private float ocrRenderDpi;

    @Override
    public List<ResumeDocument> extractDocumentsFromPdf(byte[] fileBytes, Map<String, Object> baseMetadata) {
        log.debug("Start PDF OCR through Python service, model={}, fileSize={} bytes", ocrModel, fileBytes.length);

        try (PDDocument pdfDocument = Loader.loadPDF(fileBytes)) {
            int pageCount = pdfDocument.getNumberOfPages();
            int pagesToProcess = Math.min(pageCount, ocrMaxPages);

            if (pageCount > ocrMaxPages) {
                log.warn("PDF page count {} exceeds OCR max pages {}, processing first {} pages",
                        pageCount, ocrMaxPages, pagesToProcess);
            }

            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            List<ResumeDocument> documents = new ArrayList<>();
            int totalEffectiveChars = 0;

            for (int i = 0; i < pagesToProcess; i++) {
                int pageNumber = i + 1;
                String pageText = requestOcrText(renderPageToDataUrl(renderer, i), pageNumber);
                int effectiveChars = countEffectiveChars(pageText);
                totalEffectiveChars += effectiveChars;

                Map<String, Object> metadata = new HashMap<>(baseMetadata);
                metadata.put(ResumeConstant.METADATA_KEY_PAGE_NUMBER, pageNumber);
                metadata.put("ocr_enabled", true);
                metadata.put("ocr_model", ocrModel);
                metadata.put("ocr_runtime", "python");

                documents.add(new ResumeDocument(UUID.randomUUID().toString(), pageText, metadata));
            }

            log.debug("PDF OCR completed through Python service, pages={}, effectiveChars={}",
                    documents.size(), totalEffectiveChars);
            return documents;
        } catch (IOException e) {
            log.warn("PDF rendering failed before OCR: {}", e.getMessage(), e);
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: PDF page rendering failed", e);
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
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("request_id", "resume-ocr-page-" + pageNumber + "-" + UUID.randomUUID());
            payload.put("page_number", pageNumber);
            payload.put("model", ocrModel);
            payload.put("instruction", OCR_INSTRUCTION);
            payload.put("image_data_url", imageDataUrl);

            JsonNode data = pythonResumeAiClient.ocrPage(payload);
            String text = data.path("text").asText("");
            log.debug("Python OCR page {} completed, textLength={}", pageNumber, text.length());
            return cleanupOcrText(text);
        } catch (PythonResumeAiClient.PythonResumeClientException e) {
            log.warn("Python OCR page {} failed: {}", pageNumber, e.getMessage(), e);
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: Python OCR service failed", e);
        } catch (Exception e) {
            log.warn("Python OCR page {} failed unexpectedly: {}", pageNumber, e.getMessage(), e);
            throw new ResumeAnalysisException("RESUME_OCR_FAILED: OCR failed", e);
        }
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

    private int countEffectiveChars(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.replaceAll("\\s+", "").length();
    }
}
