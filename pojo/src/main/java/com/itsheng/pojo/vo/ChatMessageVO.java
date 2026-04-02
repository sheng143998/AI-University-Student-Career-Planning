package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天消息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天消息")
public class ChatMessageVO {

    @Schema(description = "消息ID", example = "1")
    private Long id;

    @Schema(description = "会话ID", example = "1")
    private Long conversationId;

    @Schema(description = "角色：user/assistant", example = "user")
    private String role;

    @Schema(description = "消息内容", example = "请帮我分析一下我的简历")
    private String content;

    @Schema(description = "创建时间", example = "2024-01-15 10:30:00")
    private String createdAt;
}
