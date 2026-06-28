package com.community.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI communityOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("社群平台 API")
                        .version("1.0")
                        .description("Discord-like 社群平台接口文档"))
                .servers(List.of(new Server().url("http://localhost:8080").description("Gateway 统一入口")))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("输入登录接口返回的 token，格式: Bearer &lt;token&gt;")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
