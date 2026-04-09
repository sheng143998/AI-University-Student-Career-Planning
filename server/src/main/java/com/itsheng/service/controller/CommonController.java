package com.itsheng.service.controller;


import com.itsheng.common.exception.FileUploadException;
import com.itsheng.common.result.Result;
import com.itsheng.common.utils.AliOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

//通用接口
@RestController
@RequestMapping("/api/common")
@Tag(name="通用接口")
@Slf4j
@RequiredArgsConstructor
public class CommonController {


    private  final AliOssUtil aliOssUtil;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "文件上传", description = "上传文件并返回访问 URL")
    public Result<String> upload(@RequestPart("file") MultipartFile file)
    {
        log.info("文件上传 :{}",file);
        try {
            String originalFilename = file.getOriginalFilename();
            String extention = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + extention;
            String filePath = aliOssUtil.upload(file.getBytes(), newFileName);
            return Result.success(filePath);
        } catch (IOException e) {
            log.error("文件上传失败{}",e);
            throw new FileUploadException("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 从 OSS 下载文件
     * @param fileUrl 文件 URL
     * @return 文件字节数组
     */
    public byte[] download(String fileUrl) {
        log.info("从 OSS 下载文件: {}", fileUrl);
        return aliOssUtil.download(fileUrl);
    }


}