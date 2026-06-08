package com.itsheng.service.controller;

import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.AiRagFeedbackDTO;
import com.itsheng.pojo.dto.AiRagSettingsDTO;
import com.itsheng.pojo.vo.AiRagFeedbackVO;
import com.itsheng.pojo.vo.AiRagSettingsVO;
import com.itsheng.service.service.AiRagFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "AI/RAG Feedback", description = "AI/RAG 反馈与个性化设置闭环")
public class AiRagFeedbackController {

    private final AiRagFeedbackService aiRagFeedbackService;

    @PostMapping("/api/feedback/ai-rag")
    @Operation(summary = "提交 AI/RAG 结果反馈")
    public Result<AiRagFeedbackVO> submitAiRagFeedback(@RequestBody AiRagFeedbackDTO dto) {
        return Result.success(aiRagFeedbackService.submitFeedback(dto));
    }

    @GetMapping("/api/settings/ai-rag")
    @Operation(summary = "获取 AI/RAG 个性化设置")
    public Result<AiRagSettingsVO> getAiRagSettings() {
        return Result.success(aiRagFeedbackService.getSettings());
    }

    @PutMapping("/api/settings/ai-rag")
    @Operation(summary = "更新 AI/RAG 个性化设置")
    public Result<AiRagSettingsVO> updateAiRagSettings(@RequestBody AiRagSettingsDTO dto) {
        return Result.success(aiRagFeedbackService.updateSettings(dto));
    }
}
