package com.finz.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI finzOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FiNZ API")
                        .description("FiNZ 재무 관리 애플리케이션 API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("FiNZ Team")
                                .email("contact@finz.com")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.finz-site.shop")
                                .description("운영 서버")
                ));
    }
}
