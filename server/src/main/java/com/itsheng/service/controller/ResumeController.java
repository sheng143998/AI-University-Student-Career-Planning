package com.itsheng.service.controller;

import com.itsheng.common.context.BaseContext;
import com.itsheng.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/resume")
@Tag(name="简历接口")
@RequiredArgsConstructor
public class ResumeController {

    // TODO: 等待具体实现
    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) {
        // 通过 BaseContext 获取当前登录用户的 ID
        Long userId = BaseContext.getUserId();
        log.info("用户 ID:{},上传简历:{}" , userId, file.getOriginalFilename());

        return Result.success();
    }
}
