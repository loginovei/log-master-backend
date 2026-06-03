package ru.loginov.log_master.config;

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
                        .title("Log Master API")
                        .description("API системы централизованного хранения и поиска логов")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Loginov")
                                .email("loginov@example.com")));
    }
}
