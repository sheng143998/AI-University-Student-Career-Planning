package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 岗位向量存储实体
 * 对应数据库表：job_vector_store
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobVectorStore {

    /**
     * 主键 ID (UUID)
     */
    private String id;

    /**
     * 关联 recruitment_data.id
     */
    private Long jobId;

    /**
     * 岗位内容 (合并岗位名称、详情等)
     */
    private String content;

    /**
     * embedding 向量 (1024 维)，格式："[0.1,0.2,...]"
     */
    private String embeddingVector;

    /**
     * 元数据 (JSON 格式)
     */
    private String metadata;

    /**
     * 内容哈希 (用于去重)
     */
    private String contentHash;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
