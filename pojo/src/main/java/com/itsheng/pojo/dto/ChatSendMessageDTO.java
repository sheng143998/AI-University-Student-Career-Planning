package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
@Schema(description = "发送消息请求")
public class ChatSendMessageDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID", example = "1")
    private Long conversationId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "请帮我分析一下我的简历")
    private String content;

    @Schema(description = "可选：当前选中的简历分析记录ID（resume_analysis_result.id）", example = "123")
    private Long resumeId;
}
