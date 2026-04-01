package com.itsheng.service.service;

import com.itsheng.pojo.dto.ChatCreateConversationDTO;
import com.itsheng.pojo.dto.ChatSendMessageDTO;
import com.itsheng.pojo.vo.ChatConversationVO;
import com.itsheng.pojo.vo.ChatDailySuggestionsVO;
import com.itsheng.pojo.vo.ChatMessageVO;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {

    /**
     * 创建会话
     * @param dto 创建会话请求
     * @return 会话VO
     */
    ChatConversationVO createConversation(ChatCreateConversationDTO dto);

    /**
     * 获取会话列表
     * @param cursor 游标
     * @param limit 每页数量
     * @return 会话列表
     */
    List<ChatConversationVO> getConversations(Long cursor, Integer limit);

    /**
     * 获取会话消息列表
     * @param conversationId 会话ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 消息列表
     */
    List<ChatMessageVO> getMessages(Long conversationId, Long cursor, Integer limit);

    /**
     * 发送消息（流式返回AI响应）
     * @param dto 发送消息请求
     * @return AI响应流
     */
    Flux<String> sendMessage(ChatSendMessageDTO dto);

    /**
     * 删除会话
     * @param conversationId 会话ID
     */
    void deleteConversation(Long conversationId);

    /**
     * 获取每日建议（基于用户简历）
     * @param resumeId 简历ID（可选，不传则使用最新简历）
     * @return 每日建议VO
     */
    ChatDailySuggestionsVO getDailySuggestions(Long resumeId);

    /**
     * 上传聊天附件
     * @param file 文件
     * @return 文件URL
     */
    String uploadAttachment(MultipartFile file);

    /**
     * 语音输入（上传音频转写为文本）
     * @param file 音频文件
     * @return 转写后的文本
     */
    String voiceToText(MultipartFile file);
}
