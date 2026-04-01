package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话列表项 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会话列表项")
public class ChatConversationVO {

    @Schema(description = "会话ID", example = "1")
    private Long id;

    @Schema(description = "会话标题", example = "简历优化讨论")
    private String title;

    @Schema(description = "最后消息时间", example = "2024-01-15 10:30:00")
    private String lastMessageAt;

    @Schema(description = "创建时间", example = "2024-01-15 09:00:00")
    private String createdAt;
}
