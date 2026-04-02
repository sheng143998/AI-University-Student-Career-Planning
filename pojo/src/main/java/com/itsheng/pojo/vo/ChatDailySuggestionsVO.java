package com.itsheng.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 每日建议 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "每日建议")
public class ChatDailySuggestionsVO {

    @Schema(description = "建议列表")
    private List<SuggestionItem> suggestions;

    @Schema(description = "推荐提问列表")
    private List<QuickQuestion> quickQuestions;

    @Schema(description = "建议项")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SuggestionItem {
        @Schema(description = "建议标题", example = "简历优化建议")
        private String title;

        @Schema(description = "建议内容", example = "根据您的简历分析，建议增加项目经验描述")
        private String text;
    }

    @Schema(description = "快捷提问")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickQuestion {
        @Schema(description = "问题标题", example = "简历评分")
        private String title;

        @Schema(description = "问题内容", example = "我的简历评分是多少？")
        private String text;
    }
}
