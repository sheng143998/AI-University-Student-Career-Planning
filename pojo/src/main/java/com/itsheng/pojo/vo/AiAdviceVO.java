package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "AI建议")
public class AiAdviceVO {
    
    @Schema(description = "AI建议内容", example = "根据你的进度，建议在下周开始准备系统架构设计相关的深度学习。")
    private String content;
}
