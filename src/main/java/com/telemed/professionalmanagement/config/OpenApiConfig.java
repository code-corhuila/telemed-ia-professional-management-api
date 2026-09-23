package com.telemed.professionalmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI telemedOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TeleMed IA - Professional Management API")
                        .version("v1.0.0")
                        .description("REST API for the Professional Management bounded context."))
                .servers(List.of(new Server().url("/").description("Default server")));
    }
}
