package com.itsheng.service.controller;

import com.itsheng.common.constant.JwtClaimsConstant;
import com.itsheng.common.properties.JwtProperties;
import com.itsheng.common.result.Result;
import com.itsheng.common.utils.JwtUtil;
import com.itsheng.pojo.dto.UserLoginDTO;
import com.itsheng.pojo.dto.UserRegisterDTO;
import com.itsheng.pojo.entity.User;
import com.itsheng.pojo.vo.UserLoginVO;
import com.itsheng.pojo.vo.UserRegisterVO;
import com.itsheng.service.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name="用户接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    public Result<UserRegisterVO> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        log.info("用户注册{}", userRegisterDTO);
        UserRegisterVO userRegisterVO =  userService.register(userRegisterDTO);
        return  Result.success(userRegisterVO);
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        log.info("用户登录{}", userLoginDTO);
        User user = userService.login(userLoginDTO);
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getSecret(),
                jwtProperties.getTtl(),
                claims);
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .username(user.getUsername())
                .sex(user.getSex())
                .userimage(user.getUserimage())
                .token(token)
                .build();
        return Result.success(userLoginVO);
    }
}
