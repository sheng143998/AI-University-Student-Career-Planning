package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.dto.UserProfileUpdateDTO;
import com.itsheng.pojo.vo.UserProfileDetailVO;
import com.itsheng.pojo.vo.UserProfileVO;
import com.itsheng.service.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户档案 Controller
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户档案接口")
@RequiredArgsConstructor
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取个人档案概览
     * @return 档案概览 VO
     */
    @GetMapping("/profile")
    @Operation(summary = "获取个人档案概览", description = "获取当前登录用户的个人档案概览信息，包括头像、定位、当前/目标岗位、匹配分等基础字段")
    public Result<UserProfileVO> getProfile() {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取个人档案概览", userId);

        UserProfileVO result = userProfileService.getProfile();
        return result != null ? Result.success(result) : Result.error("未找到用户档案");
    }

    /**
     * 更新个人档案概览
     * @param dto 更新参数
     * @return 更新结果
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人档案概览", description = "更新用户个人档案概览信息，包括昵称、头像 URL。注意：location、current_role、target_role、match_score 等字段来自简历 AI 分析结果，不支持手动修改")
    public Result<Void> updateProfile(@RequestBody UserProfileUpdateDTO dto) {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 更新个人档案概览，dto: {}", userId, dto);

        boolean updated = userProfileService.updateProfile(dto);
        return updated ? Result.success() : Result.error("更新失败");
    }

    /**
     * 获取详细档案
     * @return 详细档案 VO
     */
    @GetMapping("/profile/detail")
    @Operation(summary = "获取详细档案", description = "获取当前登录用户的详细档案信息，包括教育经历、工作经历、技能列表、项目经历等。数据从简历 AI 分析结果中提取")
    public Result<UserProfileDetailVO> getProfileDetail() {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取详细档案", userId);

        UserProfileDetailVO result = userProfileService.getProfileDetail();
        return Result.success(result);
    }
}
