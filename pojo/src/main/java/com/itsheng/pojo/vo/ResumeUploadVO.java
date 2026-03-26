package com.itsheng.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简历上传接口返回 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadVO {
    /**
     * 记录 ID
     */
    private String id;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 简历文件存储路径（OSS 路径）
     */
    private String resumeFilePath;

    /**
     * 解析状态：PROCESSING / COMPLETED / FAILED
     */
    private String parsingStatus;

    /**
     * 创建时间
     */
    private String createdAt;
}
