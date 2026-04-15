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
            "pdf", "doc", "docx", "txt", "md"
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
     * PDF 解析配置 - 每 2 页作为一个 Document，避免工作经历跨页被切断
     * 如果简历内容较长，可增加此值至 3
     */
    public static final int PDF_PAGES_PER_DOCUMENT = 2;

    /**
     * PDF 页面重叠数 - 相邻 Document 之间重叠 1 页，确保跨页内容不丢失
     */
    public static final int PDF_PAGE_OVERLAP = 1;

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

    /**
     * 可直接编辑的简历文件格式（docx 因 XML 结构复杂不支持自动编辑）
     */
    public static final java.util.Set<String> EDITABLE_RESUME_FILE_TYPES = java.util.Set.of("txt", "md");

    /**
     * 技能区块识别关键词
     */
    public static final java.util.Set<String> SKILL_SECTION_KEYWORDS = java.util.Set.of(
            "专业技能", "个人技能", "技能清单", "技术栈", "掌握技能"
    );

    /**
     * 下一个区块的标题关键词（遇到这些则停止收集技能段落）
     */
    public static final java.util.Set<String> NEXT_SECTION_KEYWORDS = java.util.Set.of(
            "教育经历", "工作经历", "项目经历", "实习经历", "自我评价",
            "获奖情况", "语言能力", "自我介绍", "个人简介", "荣誉奖项"
    );

    /**
     * 当没有标题匹配时，回退密度算法的最小匹配数
     */
    public static final int MIN_SKILL_MATCH_THRESHOLD = 2;

    private ResumeConstant() {
        // 私有构造器，防止实例化
    }
}
