package com.itsheng.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "fuchuang.jwt")
public class JwtProperties {
    private String secret;
    private Long ttl;
    private String tokenName;
}