package com.itsheng.service.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 简历解析得到的轻量文本片段，避免 Java 业务层依赖模型框架文档类型。
 */
public class ResumeDocument {

    private final String id;
    private final String text;
    private final Map<String, Object> metadata;

    public ResumeDocument(String id, String text, Map<String, Object> metadata) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.text = text == null ? "" : text;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public static ResumeDocument of(String text, Map<String, Object> metadata) {
        return new ResumeDocument(UUID.randomUUID().toString(), text, metadata);
    }

    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
