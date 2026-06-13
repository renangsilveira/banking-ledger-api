package com.example.ledger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Banking Ledger API")
                .description("Production-grade double-entry banking ledger API built with Java 21 and Spring Boot 3")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Renan Silveira")
                    .url("https://github.com/renangsilveira")));
    }
}
