package com.itsheng.service.mapper;

import com.itsheng.pojo.entity.ResumeAnalysisResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 简历 Mapper 接口
 */
@Mapper
public interface ResumeMapper {

    /**
     * 插入简历分析结果
     * @param result 简历分析结果
     * @return 影响行数
     */
    int insert(ResumeAnalysisResult result);

    /**
     * 根据 vectorStoreId 查询分析结果
     * @param vectorStoreId 向量存储 ID
     * @return 简历分析结果
     */
    ResumeAnalysisResult selectByVectorStoreId(@Param("vectorStoreId") String vectorStoreId);

    /**
     * 按简历分析记录主键和用户ID查询
     * @param id 简历分析记录ID（resume_analysis_result.id）
     * @param userId 用户ID
     * @return 简历分析记录
     */
    ResumeAnalysisResult selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 根据 userId 查询分析结果列表（游标分页）
     * @param userId 用户 ID
     * @param cursor 游标（id 值）
     * @param limit 每页数量
     * @return 简历分析结果列表
     */
    List<ResumeAnalysisResult> selectByUserId(@Param("userId") Long userId,
                                               @Param("cursor") Long cursor,
                                               @Param("limit") Integer limit);

    /**
     * 动态更新分析结果（支持更新 status、progress、parsedData、scores、highlights、suggestions 等字段）
     * @param result 简历分析结果
     * @return 影响行数
     */
    int update(ResumeAnalysisResult result);
}
