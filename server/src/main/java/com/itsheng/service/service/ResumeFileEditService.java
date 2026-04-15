package com.itsheng.service.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.lang.Nullable;

/**
 * 简历文件直接编辑服务。
 * 支持 docx / txt / md 格式，修改后重新上传 OSS。
 */
public interface ResumeFileEditService {

    /**
     * 尝试编辑简历文件。
     *
     * @param fileUrl      文件 OSS URL
     * @param field        要编辑的字段
     * @param oldValueNode 旧值
     * @param newValueNode 新值
     * @param userId       用户 ID
     * @return 新文件 URL，编辑失败返回 null
     */
    @Nullable
    String tryEditResumeFile(String fileUrl, String field,
                             JsonNode oldValueNode, JsonNode newValueNode,
                             Long userId);

    /**
     * 从 OSS URL 提取文件扩展名（小写，不含点）。
     */
    String getExtension(String url);

    /**
     * 读取简历文件全文（仅支持 txt/md），供 AI 参考原文排版风格。
     */
    @Nullable
    String readResumeFileRaw(String fileUrl);

    /**
     * 读取简历文件中专业技能区块的原始文本（仅支持 txt/md）。
     * 返回从标题行（含）到下一区块标题（不含）的原文，供 AI 学习排版风格。
     */
    @Nullable
    String readSkillsSectionRaw(String fileUrl);
}
