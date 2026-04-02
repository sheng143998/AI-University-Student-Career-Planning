package com.itsheng.service.controller;

import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.ChatCreateConversationDTO;
import com.itsheng.pojo.dto.ChatSendMessageDTO;
import com.itsheng.pojo.vo.ChatConversationVO;
import com.itsheng.pojo.vo.ChatDailySuggestionsVO;
import com.itsheng.pojo.vo.ChatMessageVO;
import com.itsheng.service.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天 REST 接口
 */
@RestController
@RequestMapping("/api/chat")
@Tag(name = "聊天接口")
@Slf4j
@RequiredArgsConstructor
public class ChatRestController {

    private final ChatService chatService;

    /**
     * 创建会话
     */
    @PostMapping("/conversations")
    @Operation(summary = "创建会话", description = "创建一个新的聊天会话")
    public Result<ChatConversationVO> createConversation(@Valid @RequestBody ChatCreateConversationDTO dto) {
        log.info("创建会话: {}", dto.getTitle());
        ChatConversationVO vo = chatService.createConversation(dto);
        return Result.success(vo);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表", description = "获取当前用户的会话列表，支持游标分页")
    public Result<List<ChatConversationVO>> getConversations(
            @Parameter(description = "游标（上一页最后一条记录的ID）") @RequestParam(required = false) Long cursor,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<ChatConversationVO> conversations = chatService.getConversations(cursor, limit);
        return Result.success(conversations);
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "获取会话消息", description = "获取指定会话的消息列表")
    public Result<List<ChatMessageVO>> getMessages(
            @Parameter(description = "会话ID") @PathVariable Long conversationId,
            @Parameter(description = "游标") @RequestParam(required = false) Long cursor,
            @Parameter(description = "每页数量") @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<ChatMessageVO> messages = chatService.getMessages(conversationId, cursor, limit);
        return Result.success(messages);
    }

    /**
     * 发送消息（流式响应）
     */
    @PostMapping(value = "/messages", produces = MediaType.TEXT_HTML_VALUE + ";charset=utf-8")
    @Operation(summary = "发送消息", description = "发送消息并获取 AI 流式响应")
    public Flux<String> sendMessage(@Valid @RequestBody ChatSendMessageDTO dto) {
        log.info("发送消息到会话 {}: {}", dto.getConversationId(), dto.getContent());
        return chatService.sendMessage(dto);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除会话", description = "删除指定会话及其所有消息")
    public Result<Void> deleteConversation(
            @Parameter(description = "会话ID") @PathVariable Long conversationId) {
        log.info("删除会话: {}", conversationId);
        chatService.deleteConversation(conversationId);
        return Result.success();
    }

    /**
     * 获取每日建议
     */
    @GetMapping("/daily-suggestions")
    @Operation(summary = "获取每日建议", description = "基于用户简历生成今日建议和推荐提问，可指定简历ID")
    public Result<ChatDailySuggestionsVO> getDailySuggestions(
            @Parameter(description = "简历ID（可选，不传则使用最新简历）") @RequestParam(required = false) Long resumeId) {
        ChatDailySuggestionsVO suggestions = chatService.getDailySuggestions(resumeId);
        return Result.success(suggestions);
    }

    /**
     * 上传聊天附件
     */
    @PostMapping(value = "/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传聊天附件", description = "上传文件作为聊天附件，返回文件 URL")
    public Result<String> uploadAttachment(@RequestPart("file") MultipartFile file) {
        log.info("上传聊天附件: {}", file.getOriginalFilename());
        String url = chatService.uploadAttachment(file);
        return Result.success(url);
    }

    /**
     * 语音转文字
     */
    @PostMapping(value = "/voice", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "语音输入", description = "上传音频文件，转写为文字")
    public Result<String> voiceToText(@RequestPart("file") MultipartFile file) {
        log.info("语音转写: {}", file.getOriginalFilename());
        String text = chatService.voiceToText(file);
        return Result.success(text);
    }
}
