package com.itsheng.service.service.Impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itsheng.common.context.BaseContext;
import com.itsheng.pojo.dto.ResumeParsedData;
import com.itsheng.pojo.dto.UserProfileUpdateDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserProfileDetailVO;
import com.itsheng.pojo.vo.UserProfileVO;
import com.itsheng.service.mapper.UserProfileMapper;
import com.itsheng.service.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户档案 Service 实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;

    @Override
    public UserProfileVO getProfile() {
        // 获取当前用户 ID
        Long userId = BaseContext.getUserId();
        log.info("获取用户档案概览，userId: {}", userId);

        // 查询用户基本信息
        User user = userProfileMapper.selectById(userId);
        if (user == null) {
            log.warn("用户不存在，userId: {}", userId);
            return null;
        }

        // 查询最新的简历分析结果中的 parsed_data
        String parsedDataJson = userProfileMapper.selectLatestParsedDataByUserId(userId);

        // 构建返回 VO
        UserProfileVO.UserProfileVOBuilder builder = UserProfileVO.builder()
                .id(user.getId())
                .name(user.getUsername())
                .avatar(user.getUserimage());

        // 如果存在简历分析数据，从中提取字段
        if (parsedDataJson != null && !parsedDataJson.isEmpty() && !parsedDataJson.equals("{}")) {
            try {
                ResumeParsedData parsedData = objectMapper.readValue(parsedDataJson, ResumeParsedData.class);
                builder.location(parsedData.getLocation());
                builder.currentRole(parsedData.getCurrentRole());
                builder.targetRole(parsedData.getTargetRole());
                builder.matchScore(parsedData.getMatchScore());
            } catch (JsonProcessingException e) {
                log.warn("解析 parsed_data 失败：{}", e.getMessage());
            }
        }

        return builder.build();
    }

    @Override
    public boolean updateProfile(UserProfileUpdateDTO dto) {
        // 获取当前用户 ID
        Long userId = BaseContext.getUserId();
        log.info("更新用户档案概览，userId: {}, dto: {}", userId, dto);

        // 更新用户基本信息（name 和 avatar）
        int rows = userProfileMapper.updateUserBaseInfo(
                userId,
                dto.getName(),
                dto.getAvatar()
        );

        return rows > 0;
    }

    @Override
    public UserProfileDetailVO getProfileDetail() {
        // 获取当前用户 ID
        Long userId = BaseContext.getUserId();
        log.info("获取用户详细档案，userId: {}", userId);

        // 查询最新的简历分析结果中的 parsed_data
        String parsedDataJson = userProfileMapper.selectLatestParsedDataByUserId(userId);

        // 如果没有简历分析数据，返回空对象
        if (parsedDataJson == null || parsedDataJson.isEmpty() || parsedDataJson.equals("{}")) {
            log.info("用户暂无简历分析数据，userId: {}", userId);
            return UserProfileDetailVO.builder()
                    .education(null)
                    .experience(null)
                    .skills(null)
                    .projects(null)
                    .build();
        }

        try {
            ResumeParsedData parsedData = objectMapper.readValue(parsedDataJson, ResumeParsedData.class);

            // 将 parsed_data 中的 education 转换为 UserProfileDetailVO.EducationItem
            var educationItems = parsedData.getEducation() != null ?
                    parsedData.getEducation().stream()
                            .map(e -> UserProfileDetailVO.EducationItem.builder()
                                    .school(e.getSchool())
                                    .major(e.getMajor())
                                    .degree(e.getDegree())
                                    .period(e.getPeriod())
                                    .build())
                            .toList() : null;

            // 将 parsed_data 中的 experience 转换为 UserProfileDetailVO.ExperienceItem
            var experienceItems = parsedData.getExperience() != null ?
                    parsedData.getExperience().stream()
                            .map(e -> UserProfileDetailVO.ExperienceItem.builder()
                                    .company(e.getCompany())
                                    .position(e.getPosition())
                                    .period(e.getPeriod())
                                    .description(e.getDescription())
                                    .build())
                            .toList() : null;

            // 构建并返回 VO
            return UserProfileDetailVO.builder()
                    .education(educationItems)
                    .experience(experienceItems)
                    .skills(parsedData.getSkills())
                    .projects(null) // projects 字段在当前 parsed_data 结构中不存在
                    .build();

        } catch (JsonProcessingException e) {
            log.error("解析 parsed_data 失败：{}", e.getMessage(), e);
            return UserProfileDetailVO.builder()
                    .education(null)
                    .experience(null)
                    .skills(null)
                    .projects(null)
                    .build();
        }
    }
}
