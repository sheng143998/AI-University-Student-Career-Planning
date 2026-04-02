package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天消息 Mapper 接口
 */
@Mapper
public interface ChatMessageMapper {

    /**
     * 插入消息
     * @param message 消息实体
     * @return 影响行数
     */
    int insert(ChatMessage message);

    /**
     * 根据会话ID查询消息列表（按创建时间正序）
     * @param conversationId 会话ID
     * @param cursor 游标（id值）
     * @param limit 每页数量
     * @return 消息列表
     */
    List<ChatMessage> selectByConversationId(@Param("conversationId") Long conversationId,
                                              @Param("cursor") Long cursor,
                                              @Param("limit") Integer limit);

    /**
     * 根据ID查询消息
     * @param id 消息ID
     * @return 消息实体
     */
    ChatMessage selectById(@Param("id") Long id);

    /**
     * 删除会话的所有消息
     * @param conversationId 会话ID
     * @return 影响行数
     */
    int deleteByConversationId(@Param("conversationId") Long conversationId);
}
