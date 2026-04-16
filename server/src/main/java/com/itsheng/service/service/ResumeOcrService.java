package com.itsheng.service.service;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface ResumeOcrService {

    /**
     * 对疑似图片型 PDF 执行 OCR，返回页级 Document 列表。
     */
    List<Document> extractDocumentsFromPdf(byte[] fileBytes, Map<String, Object> baseMetadata);
}
