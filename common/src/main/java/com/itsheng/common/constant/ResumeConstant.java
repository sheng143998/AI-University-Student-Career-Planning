package com.itsheng.common.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 简历上传相关常量
 */
public class ResumeConstant {

    /**
     * 允许上传的文件类型
     */
    public static final Set<String> ALLOWED_FILE_TYPES = new HashSet<>(Arrays.asList(
            "pdf", "docx", "pptx", "html", "txt"
    ));

    /**
     * 最大文件大小 (10MB)
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 简历解析状态
     */
    public static final String PARSING_STATUS_PROCESSING = "PROCESSING";

    /**
     * PDF 解析配置 - 每页作为一个 Document
     */
    public static final int PDF_PAGES_PER_DOCUMENT = 1;

    /**
     * 文本分割配置
     */
    public static final int TEXT_SPLITTER_CHUNK_SIZE = 800;
    public static final int TEXT_SPLITTER_MIN_CHUNK_SIZE_CHARS = 350;
    public static final int TEXT_SPLITTER_MIN_CHUNK_LENGTH_TO_EMBED = 10;
    public static final int TEXT_SPLITTER_MAX_NUM_CHUNKS = 5000;

    /**
     * Metadata 键名
     */
    public static final String METADATA_KEY_USER_ID = "user_id";
    public static final String METADATA_KEY_FILE_PATH = "file_path";
    public static final String METADATA_KEY_FILE_TYPE = "file_type";
    public static final String METADATA_KEY_DOCUMENT_ID = "document_id";
    public static final String METADATA_KEY_PAGE_NUMBER = "page_number";
    public static final String METADATA_KEY_CHUNK_NUMBER = "chunk_number";

    private ResumeConstant() {
        // 私有构造器，防止实例化
    }
}
