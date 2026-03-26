package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import com.itsheng.pojo.vo.ResumeUploadVO;
import com.itsheng.service.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@Tag(name = "简历接口")
@RequiredArgsConstructor
public class ResumeController {

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
}
