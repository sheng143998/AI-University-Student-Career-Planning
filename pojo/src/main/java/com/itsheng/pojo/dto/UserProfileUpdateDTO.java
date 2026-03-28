package com.itsheng.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户档案更新 DTO
 * 对应接口文档 2.2 更新个人档案概览的请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {

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
}
