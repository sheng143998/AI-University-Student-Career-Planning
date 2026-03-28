package com.itsheng.service.controller;

import com.itsheng.common.constant.JwtClaimsConstant;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.properties.JwtProperties;
import com.itsheng.common.result.Result;
import com.itsheng.common.utils.JwtUtil;
import com.itsheng.pojo.dto.UserDTO;
import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.vo.UserLoginVO;
import com.itsheng.pojo.vo.UserVO;
import com.itsheng.service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<UserVO> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("用户注册{}", userRegisterDTO);
        UserVO userVO = userService.register(userRegisterDTO);
        return Result.success(userVO);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录{}", userLoginDTO);
        UserVO userVO = userService.login(userLoginDTO);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, userVO.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecret(),
                jwtProperties.getTtl(),
                claims);
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .token(token)
                .username(userVO.getUsername())
                .sex(userVO.getSex())
                .userImage(userVO.getUserImage())
                .createtime(LocalDateTime.now())
                .build();
        return Result.success(userLoginVO);
    }

    @GetMapping("/info")
    @Operation(summary = "获取用户信息")
    public Result<UserVO> getUserInfo() {
        Long userId = BaseContext.getUserId();
        log.info("获取用户信息，userId:{}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    @PutMapping("/edit")
    @Operation(summary = "编辑用户信息")
    public Result<UserVO> editUser(@RequestBody UserDTO userDTO) {
        Long userId = BaseContext.getUserId();
        log.info("编辑用户信息，userId:{}, userDTO:{}", userId, userDTO);
        UserVO userVO = userService.editUser(userId, userDTO);
        return Result.success(userVO);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        log.info("用户退出登录，userId:{}", BaseContext.getUserId());
        userService.logout();
        return Result.success();
    }
}
