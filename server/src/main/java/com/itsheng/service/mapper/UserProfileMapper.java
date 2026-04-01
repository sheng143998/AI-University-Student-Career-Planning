package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户档案 Mapper 接口
 * 从 users 表和 resume_analysis_result 表获取用户画像数据
 */
@Mapper
public interface UserProfileMapper {

    /**
     * 根据用户 ID 查询用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    User selectById(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询最新的简历分析结果中的 parsed_data
     * @param userId 用户 ID
     * @return 简历分析结果中的 parsed_data(JSON 字符串)
     */
    String selectLatestParsedDataByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询最新的简历分析结果中的 scores
     * @param userId 用户 ID
     * @return 简历分析结果中的 scores(JSON 字符串)
     */
    String selectLatestScoresByUserId(@Param("userId") Long userId);

    /**
     * 根据用户 ID 查询最新的简历分析记录 ID
     * @param userId 用户 ID
     * @return 简历分析记录 ID
     */
    Long selectLatestAnalysisIdByUserId(@Param("userId") Long userId);

    /**
     * 根据简历分析记录 ID 查询 parsed_data
     * @param resumeId 简历分析记录 ID
     * @param userId 用户 ID（用于权限校验）
     * @return 简历分析结果中的 parsed_data(JSON 字符串)
     */
    String selectParsedDataByResumeId(@Param("resumeId") Long resumeId, @Param("userId") Long userId);

    /**
     * 更新用户基本信息
     * @param userId 用户 ID
     * @param userName 用户名
     * @param userImage 用户头像
     * @return 更新行数
     */
    int updateUserBaseInfo(@Param("userId") Long userId, @Param("userName") String userName, @Param("userImage") String userImage);
}
