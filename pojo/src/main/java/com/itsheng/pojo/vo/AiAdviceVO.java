package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI建议")
public class AiAdviceVO {
    
    @Schema(description = "AI建议内容", example = "根据你的进度，建议在下周开始准备系统架构设计相关的深度学习。")
    private String content;

    @Schema(description = "RAG 证据引用，仅生成接口实时返回，overview/detail 读取数据库时可为空")
    private List<Map<String, Object>> evidenceReferences;

    @Schema(description = "RAG 检索诊断，仅生成接口实时返回，overview/detail 读取数据库时可为空")
    private Map<String, Object> retrievalDiagnostics;

    public AiAdviceVO(String content) {
        this.content = content;
    }
}
