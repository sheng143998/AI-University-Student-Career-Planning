package com.itsheng.service.service.Impl;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.exception.FileUploadException;
import com.itsheng.pojo.dto.ChatCreateConversationDTO;
import com.itsheng.pojo.dto.ChatSendMessageDTO;
import com.itsheng.pojo.entity.ChatConversation;
import com.itsheng.pojo.entity.ChatMessage;
import com.itsheng.pojo.vo.ChatConversationVO;
import com.itsheng.pojo.vo.ChatDailySuggestionsVO;
import com.itsheng.pojo.vo.ChatMessageVO;
import com.itsheng.service.client.PythonChatClient;
import com.itsheng.service.controller.CommonController;
import com.itsheng.service.mapper.ChatConversationMapper;
import com.itsheng.service.mapper.ChatMessageMapper;
import com.itsheng.service.mapper.UserProfileMapper;
import com.itsheng.service.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 聊天服务实现类
 */
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final UserProfileMapper userProfileMapper;
    private final PythonChatClient pythonChatClient;
    private final CommonController commonController;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int DEFAULT_LIMIT = 20;
    private static final int CHAT_CONTEXT_LIMIT = 12;

    public ChatServiceImpl(ChatConversationMapper conversationMapper,
                           ChatMessageMapper messageMapper,
                           UserProfileMapper userProfileMapper,
                           PythonChatClient pythonChatClient,
                           CommonController commonController) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.userProfileMapper = userProfileMapper;
        this.pythonChatClient = pythonChatClient;
        this.commonController = commonController;
    }

    @Override
    @Transactional
    public ChatConversationVO createConversation(ChatCreateConversationDTO dto) {
        Long userId = BaseContext.getUserId();
        LocalDateTime now = LocalDateTime.now();

        ChatConversation conversation = ChatConversation.builder()
                .userId(userId)
                .title(dto.getTitle())
                .createTime(now)
                .updateTime(now)
                .build();

        conversationMapper.insert(conversation);

        return convertToConversationVO(conversation);
    }

    @Override
    public List<ChatConversationVO> getConversations(Long cursor, Integer limit) {
        Long userId = BaseContext.getUserId();
        if (limit == null || limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        List<ChatConversation> conversations = conversationMapper.selectByUserId(userId, cursor, limit);
        List<ChatConversationVO> result = new ArrayList<>();

        for (ChatConversation conv : conversations) {
            result.add(convertToConversationVO(conv));
        }

        return result;
    }

    @Override
    public List<ChatMessageVO> getMessages(Long conversationId, Long cursor, Integer limit) {
        Long userId = BaseContext.getUserId();
        if (limit == null || limit <= 0) {
            limit = DEFAULT_LIMIT;
        }

        // 验证会话归属
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        List<ChatMessage> messages = messageMapper.selectByConversationId(conversationId, cursor, limit);
        List<ChatMessageVO> result = new ArrayList<>();

        for (ChatMessage msg : messages) {
            result.add(convertToMessageVO(msg));
        }

        return result;
    }

    @Override
    @Transactional
    public Flux<String> sendMessage(ChatSendMessageDTO dto) {
        Long userId = BaseContext.getUserId();
        log.info("sendMessage received: conversationId={}, resumeId={}, contentLength={}",
                dto.getConversationId(), dto.getResumeId(), dto.getContent() == null ? 0 : dto.getContent().length());

        // 验证会话归属
        ChatConversation conversation = conversationMapper.selectById(dto.getConversationId());
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return Flux.just("错误：会话不存在或无权访问");
        }

        // 保存用户消息
        LocalDateTime now = LocalDateTime.now();
        ChatMessage userMessage = ChatMessage.builder()
                .conversationId(dto.getConversationId())
                .userId(userId)
                .role("user")
                .content(dto.getContent())
                .createTime(now)
                .build();
        messageMapper.insert(userMessage);

        // 更新会话最后消息时间
        conversationMapper.updateLastMessageAt(dto.getConversationId(), now);

        // 调用 Python RAG 服务生成 AI 回复
        List<ChatMessage> history = messageMapper.selectRecentByConversationId(dto.getConversationId(), CHAT_CONTEXT_LIMIT);
        Collections.reverse(history);
        String parsedDataJson = resolveParsedDataJson(dto.getResumeId(), userId);
        PythonChatClient.ChatCompletionResult completion = pythonChatClient.complete(
                userId,
                dto.getConversationId(),
                dto.getContent(),
                dto.getResumeId(),
                history,
                parsedDataJson
        );
        String finalContent = completion.getContent() != null ? completion.getContent() : "";

        // 保存 AI 响应
        ChatMessage assistantMessage = ChatMessage.builder()
                .conversationId(dto.getConversationId())
                .userId(userId)
                .role("assistant")
                .content(finalContent)
                .createTime(LocalDateTime.now())
                .build();
        messageMapper.insert(assistantMessage);
        conversationMapper.updateLastMessageAt(dto.getConversationId(), LocalDateTime.now());

        // 如果标题是默认的"新对话"，根据用户消息生成新标题
        if ("新对话".equals(conversation.getTitle())) {
            generateAndUpdateTitle(dto.getConversationId(), dto.getContent(), completion.getTitle());
        }

        // 将完整响应拆分为字符逐个推送，模拟流式打字机效果
        return Flux.fromArray(finalContent.split(""));
    }

    private String resolveParsedDataJson(Long resumeId, Long userId) {
        if (resumeId != null) {
            return userProfileMapper.selectParsedDataByResumeId(resumeId, userId);
        }
        return userProfileMapper.selectLatestParsedDataByUserId(userId);
    }
    
    /**
     * 根据用户消息生成对话标题
     */
    private void generateAndUpdateTitle(Long conversationId, String userMessage, String pythonTitle) {
        try {
            String title = pythonTitle;
            if (title == null || title.isBlank()) {
                title = userMessage;
            }
            // 清理标题（去掉可能的引号、换行等）
            title = title.replaceAll("[\"'\\n\\r]", "").trim();
            if (title.length() > 20) {
                title = title.substring(0, 20) + "...";
            }
            
            conversationMapper.updateTitle(conversationId, title);
            log.info("自动生成对话标题: conversationId={}, title={}", conversationId, title);
        } catch (Exception e) {
            log.warn("生成对话标题失败: {}", e.getMessage());
            // 失败时不影响主流程，保持原标题
        }
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId) {
        Long userId = BaseContext.getUserId();

        // 验证会话归属
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new RuntimeException("会话不存在或无权访问");
        }

        // 删除会话的所有消息
        messageMapper.deleteByConversationId(conversationId);
        // 删除会话
        conversationMapper.deleteById(conversationId);
    }

    @Override
    @Cacheable(value = "dailySuggestions", key = "T(java.time.LocalDate).now().toString() + ':' + #root.target.getCurrentUserId() + ':' + #resumeId")
    public ChatDailySuggestionsVO getDailySuggestions(Long resumeId) {
        Long userId = BaseContext.getUserId();
        log.info("生成每日建议: userId={}, resumeId={}", userId, resumeId);

        // 获取简历解析数据
        String parsedDataJson;
        if (resumeId != null) {
            // 根据指定简历ID获取
            parsedDataJson = userProfileMapper.selectParsedDataByResumeId(resumeId, userId);
        } else {
            // 获取最新简历解析数据
            parsedDataJson = userProfileMapper.selectLatestParsedDataByUserId(userId);
        }

        if (parsedDataJson == null || parsedDataJson.isEmpty() || "{}".equals(parsedDataJson)) {
            // 没有简历数据，返回默认建议
            return buildDefaultSuggestions();
        }

        try {
            return pythonChatClient.dailySuggestions(userId, resumeId, parsedDataJson);
        } catch (Exception e) {
            log.error("生成每日建议失败", e);
            return buildDefaultSuggestions();
        }
    }

    /**
     * 获取当前用户ID（供缓存 key 使用）
     */
    public Long getCurrentUserId() {
        return BaseContext.getUserId();
    }

    @Override
    public String uploadAttachment(MultipartFile file) {
        // 复用通用上传接口
        try {
            var result = commonController.upload(file);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
            throw new FileUploadException("文件上传失败");
        } catch (Exception e) {
            log.error("上传聊天附件失败", e);
            throw new FileUploadException("上传聊天附件失败：" + e.getMessage());
        }
    }

    @Override
    public String voiceToText(MultipartFile file) {
        // TODO: route speech transcription through a Python ASR service boundary.
        // Current implementation returns placeholder text until the Python ASR endpoint is available.
        log.info("语音转写功能待接入，文件名: {}", file.getOriginalFilename());
        return "语音转写功能待接入，请稍后使用或改用文字输入。";
    }

    /**
     * 构建默认建议（无简历数据时）
     */
    private ChatDailySuggestionsVO buildDefaultSuggestions() {
        List<ChatDailySuggestionsVO.SuggestionItem> suggestions = List.of(
                ChatDailySuggestionsVO.SuggestionItem.builder()
                        .title("上传简历")
                        .text("上传您的简历，获取个性化职业建议")
                        .build(),
                ChatDailySuggestionsVO.SuggestionItem.builder()
                        .title("职业规划")
                        .text("与 AI 讨论您的职业发展方向")
                        .build(),
                ChatDailySuggestionsVO.SuggestionItem.builder()
                        .title("技能提升")
                        .text("了解当前市场需求的热门技能")
                        .build()
        );

        List<ChatDailySuggestionsVO.QuickQuestion> quickQuestions = List.of(
                ChatDailySuggestionsVO.QuickQuestion.builder()
                        .title("简历优化")
                        .text("如何让我的简历更有竞争力？")
                        .build(),
                ChatDailySuggestionsVO.QuickQuestion.builder()
                        .title("职业方向")
                        .text("我适合什么样的工作岗位？")
                        .build(),
                ChatDailySuggestionsVO.QuickQuestion.builder()
                        .title("面试准备")
                        .text("面试时如何自我介绍？")
                        .build()
        );

        return ChatDailySuggestionsVO.builder()
                .suggestions(suggestions)
                .quickQuestions(quickQuestions)
                .build();
    }

    /**
     * 转换会话实体为 VO
     */
    private ChatConversationVO convertToConversationVO(ChatConversation conversation) {
        return ChatConversationVO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .lastMessageAt(conversation.getLastMessageAt() != null 
                        ? conversation.getLastMessageAt().format(DATE_TIME_FORMATTER) : null)
                .createdAt(conversation.getCreateTime() != null 
                        ? conversation.getCreateTime().format(DATE_TIME_FORMATTER) : null)
                .build();
    }

    /**
     * 转换消息实体为 VO
     */
    private ChatMessageVO convertToMessageVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .role(message.getRole())
                .content(message.getContent())
                .createdAt(message.getCreateTime() != null 
                        ? message.getCreateTime().format(DATE_TIME_FORMATTER) : null)
                .build();
    }
}
