package com.itsheng.service.service;

import com.itsheng.pojo.vo.ResumeAnalysisResultVO;
import com.itsheng.pojo.vo.ResumeUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历 Service 接口（包含上传、分析、预览功能）
 */
public interface ResumeService {

    /**
     * 上传简历
     * @param file 简历文件（PDF/DOCX/PPTX/HTML/TXT），最大 10MB
     * @return 上传结果 VO
     */
    ResumeUploadVO upload(MultipartFile file);

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

    /**
     * 获取简历预览响应（直接流式返回）
     *
     * @param vectorStoreId 向量存储记录 ID
     * @param disposition 响应头设置：inline / attachment
     * @return ResponseEntity 包含文件流和响应头
     */
    ResponseEntity<byte[]> preview(String vectorStoreId, String disposition);

    /**
     * 获取简历预览 URL（签名 URL）
     *
     * @param vectorStoreId 向量存储记录 ID
     * @return 签名后的预览 URL
     */
    String getPreviewUrl(String vectorStoreId);
}
