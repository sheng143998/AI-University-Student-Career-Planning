package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.ChatConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 聊天会话 Mapper 接口
 */
@Mapper
public interface ChatConversationMapper {

    /**
     * 插入会话
     * @param conversation 会话实体
     * @return 影响行数
     */
    int insert(ChatConversation conversation);

    /**
     * 根据ID查询会话
     * @param id 会话ID
     * @return 会话实体
     */
    ChatConversation selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询会话列表（按最后消息时间倒序）
     * @param userId 用户ID
     * @param cursor 游标（id值）
     * @param limit 每页数量
     * @return 会话列表
     */
    List<ChatConversation> selectByUserId(@Param("userId") Long userId,
                                          @Param("cursor") Long cursor,
                                          @Param("limit") Integer limit);

    /**
     * 更新会话标题
     * @param id 会话ID
     * @param title 新标题
     * @return 影响行数
     */
    int updateTitle(@Param("id") Long id, @Param("title") String title);

    /**
     * 更新最后消息时间
     * @param id 会话ID
     * @param lastMessageAt 最后消息时间
     * @return 影响行数
     */
    int updateLastMessageAt(@Param("id") Long id, @Param("lastMessageAt") java.time.LocalDateTime lastMessageAt);

    /**
     * 删除会话
     * @param id 会话ID
     * @return 影响行数
     */
    int deleteById(@Param("id") Long id);
}
