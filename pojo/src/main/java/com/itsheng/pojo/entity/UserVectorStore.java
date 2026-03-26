package com.itsheng.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVectorStore {
    private String id;            // VARCHAR(255) PRIMARY KEY
    private Long userId;
    private String resumeContent; // content TEXT
    private String resumeFilePath;
    private String vectorType;    // vector_type VARCHAR(50)
    private String embeddingVector; // embedding vector(1024) - pgvector 类型，使用字符串格式 "[0.1,0.2,...]"
    private String metadata;      // metadata JSONB
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
