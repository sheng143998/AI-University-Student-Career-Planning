package com.itsheng.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 创建会话请求 DTO
 */
@Data
@Schema(description = "创建会话请求")
public class ChatCreateConversationDTO {

    @NotBlank(message = "会话标题不能为空")
    @Schema(description = "会话标题", example = "简历优化讨论")
    private String title;
}
