package com.itsheng.service.config;

import com.itsheng.common.properties.JwtProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfiguration {

    private static final String SECURITY_SCHEME_NAME = "tokenAuth";

    private final JwtProperties jwtProperties;

    @Bean
    public OpenAPI openAPI() {
        String tokenHeaderName = jwtProperties.getTokenName();

        SecurityScheme tokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(tokenHeaderName);

        return new OpenAPI()
                .info(new Info().title("Service API").version("v1"))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME, tokenScheme))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
