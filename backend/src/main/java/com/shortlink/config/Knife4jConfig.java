package com.shortlink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("短链接系统 API 文档")
                        .description("短链接系统后端接口文档，包含用户管理、短链接管理、分组管理、回收站管理、监控统计等模块")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("短链接系统开发团队")
                                .email("support@shortlink.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
