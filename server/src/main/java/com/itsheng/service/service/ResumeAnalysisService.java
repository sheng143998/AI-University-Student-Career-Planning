package com.itsheng.service.service;

import com.itsheng.pojo.vo.ResumeAnalysisResultVO;

import java.util.List;

/**
 * 简历分析 Service 接口
 */
public interface ResumeAnalysisService {

    /**
     * 分析简历并保存结果
     * @param vectorStoreId 向量存储 ID
     * @param userId 用户 ID
     * @param resumeContent 简历原始内容
     * @param fileType 文件类型
     * @param originalFileName 原始文件名
     * @param resumeFilePath 文件存储路径
     */
    void analyzeAndSave(String vectorStoreId, Long userId, String resumeContent,
                        String fileType, String originalFileName, String resumeFilePath);

    /**
     * 根据 vectorStoreId 获取分析结果
     * @param vectorStoreId 向量存储 ID
     * @return 分析结果 VO
     */
    ResumeAnalysisResultVO getAnalysisResult(String vectorStoreId);

    /**
     * 获取用户的历史分析记录列表
     * @param userId 用户 ID
     * @param cursor 游标
     * @param limit 每页数量
     * @return 分析结果列表
     */
    List<ResumeAnalysisResultVO> getAnalysisList(Long userId, Long cursor, Integer limit);
}
