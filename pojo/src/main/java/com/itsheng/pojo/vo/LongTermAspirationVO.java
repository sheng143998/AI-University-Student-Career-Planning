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
@Schema(description = "长期愿景")
public class LongTermAspirationVO {
    
    @Schema(description = "愿景标题", example = "技术领导者")
    private String title;
    
    @Schema(description = "愿景描述", example = "在5年内带领20+人的技术团队，主导核心架构设计。")
    private String desc;
}
