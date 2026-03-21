package com.itsheng.service.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name="用户接口")
public class UserController {

    @PostMapping("/register")
    public String register() {
        log.info("用户注册");
        return "用户注册成功";
    }
}
