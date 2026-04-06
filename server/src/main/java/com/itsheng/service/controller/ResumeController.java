package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.vo.CapabilityProfileVO;
import com.itsheng.pojo.vo.ResumeAnalysisResultVO;
import com.itsheng.pojo.vo.ResumeUploadVO;
import com.itsheng.service.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@Tag(name = "简历接口")
@RequiredArgsConstructor
public class  ResumeController {

    private final ResumeService resumeService;

    /**
     * 上传简历
     * @param file 简历文件，支持 PDF、DOCX、PPTX、HTML、TXT 格式，最大 10MB
     * @return 上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传简历", description = "上传简历文件，后端接收后上传至 OSS 并异步解析，支持 PDF、DOCX、PPTX、HTML、TXT 格式，最大 10MB")
    public Result<ResumeUploadVO> upload(@RequestPart("file") MultipartFile file) {
        // 通过 BaseContext 获取当前登录用户的 ID
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 上传简历文件：{}", userId, file.getOriginalFilename());

        // 调用 service 上传简历
        ResumeUploadVO result = resumeService.upload(file);

        return Result.success(result);
    }

    /**
     * 获取简历分析结果
     * @param id 向量存储记录 ID（UUID）
     * @return 分析结果
     */
    @GetMapping("/analysis/{id}")
    @Operation(summary = "获取简历分析结果", description = "根据向量存储记录 ID 查询简历分析结果，解析为异步过程，前端可每隔 2s 轮询一次，最多等待 60s")
    public Result<ResumeAnalysisResultVO> getAnalysisResult(@PathVariable("id") String id) {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 查询简历分析结果：{}", userId, id);

        ResumeAnalysisResultVO result = resumeService.getAnalysisResult(id);
        return Result.success(result);
    }

    /**
     * 获取简历分析列表
     * @param cursor 游标
     * @param limit 每页数量
     * @return 分析结果列表
     */
    @GetMapping("/analysis")
    @Operation(summary = "获取简历分析列表", description = "查询当前用户的历史简历分析记录列表，支持游标分页")
    public Result<List<ResumeAnalysisResultVO>> getAnalysisList(
            @RequestParam(value = "cursor", required = false) Long cursor,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 查询简历分析列表，cursor: {}, limit: {}", userId, cursor, limit);

        List<ResumeAnalysisResultVO> result = resumeService.getAnalysisList(userId, cursor, limit);
        return Result.success(result);
    }

    /**
     * 简历预览（直接流式返回）
     * @param vectorStoreId 向量存储记录 ID
     * @param disposition 响应头设置：inline（默认）/ attachment
     * @return 文件流
     */
    @GetMapping("/analysis/{id}/preview")
    @Operation(summary = "简历预览", description = "在浏览器内嵌预览简历原件，由后端从 OSS 读取文件并返回适合浏览器内嵌展示的响应头")
    public ResponseEntity<byte[]> preview(@PathVariable("id") String vectorStoreId,
                                          @RequestParam(value = "disposition", required = false, defaultValue = "inline") String disposition) {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 预览简历，vectorStoreId: {}, disposition: {}", userId, vectorStoreId, disposition);

        return resumeService.preview(vectorStoreId, disposition);
    }

    /**
     * 获取简历预览 URL（签名 URL）
     * @param vectorStoreId 向量存储记录 ID
     * @return 签名后的预览 URL
     */
    @GetMapping("/analysis/{id}/preview-url")
    @Operation(summary = "获取简历预览 URL", description = "返回一个短期有效的 OSS 签名 URL，用于不经过应用服务器中转的直接预览")
    public Result<String> getPreviewUrl(@PathVariable("id") String vectorStoreId) {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取简历预览 URL，vectorStoreId: {}", userId, vectorStoreId);

        String url = resumeService.getPreviewUrl(vectorStoreId);
        return Result.success(url);
    }

    /**
     * 获取学生能力画像
     * @return 学生就业能力画像数据
     */
    @GetMapping("/capability-profile")
    @Operation(summary = "获取学生能力画像", description = "获取当前用户的学生就业能力画像，包含完整度评分、竞争力评分及各维度能力评估")
    public Result<CapabilityProfileVO> getCapabilityProfile() {
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{}, 获取学生能力画像", userId);

        CapabilityProfileVO result = resumeService.getCapabilityProfile(userId);
        if (result == null) {
            return Result.error("尚未上传简历，无法获取能力画像");
        }
        return Result.success(result);
    }
}
