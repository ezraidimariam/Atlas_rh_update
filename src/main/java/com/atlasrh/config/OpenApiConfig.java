package com.atlasrh.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI atlasRhOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AtlasRH HRMS API")
                .version("1.0.0")
                .description("API REST V1 de gestion RH AtlasRH. L'authentification n'est pas activée."));
    }
}
