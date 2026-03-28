package com.itsheng.pojo.vo;

import com.itsheng.pojo.dto.ResumeParsedData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户档案概览 VO
 * 对应接口文档 2.1 获取个人档案概览
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 昵称/姓名
     */
    private String name;

    /**
     * 头像 URL
     */
    private String avatar;

    /**
     * 所在城市
     */
    private String location;

    /**
     * 当前岗位
     */
    private String currentRole;

    /**
     * 目标岗位
     */
    private String targetRole;

    /**
     * 岗位匹配分（0-100）
     */
    private Integer matchScore;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
