package com.itsheng.service.config;

import com.itsheng.service.interceptor.JwtTokenInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

  private final JwtTokenInterceptor jwtTokenInterceptor;


  //注册拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login",
                        "/api/user/register",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/doc.html",
                        "/webjars/**",
                        "/error");
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("web端接口")
                .packagesToScan("com.itsheng.service.controller")
                .pathsToMatch("/**")
                .build();
    }

}
