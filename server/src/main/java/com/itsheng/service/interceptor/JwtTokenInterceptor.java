package com.itsheng.service.interceptor;

import com.itsheng.common.constant.JwtClaimsConstant;
import com.itsheng.common.context.BaseContext;
import com.itsheng.common.properties.JwtProperties;
import com.itsheng.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断当前拦截到的是 Controller 的方法还是其他资源
        if (!(handler instanceof HandlerMethod)) {
            //当前拦截到的不是动态方法，直接放行
            return true;
        }

        //1、从请求头中获取令牌
        String token = request.getHeader(jwtProperties.getTokenName());

        //2、校验令牌
        try {
            log.info("jwt 校验:{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getSecret(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            log.info("当前用户 id:{}", userId);
            // 将用户 ID 存入 ThreadLocal，方便后续业务逻辑获取
            BaseContext.setUserId(userId);
            //3、通过，放行
            return true;
        } catch (Exception ex) {
            //4、不通过，响应 401 状态码
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后，清理 ThreadLocal，避免内存泄漏
        BaseContext.removeUserId();
    }

}
