package com.beril.mailnotifier.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI mailNotifierOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MailNotifier API")
                        .description("Gmail beklenti takip ve anlık bildirim sistemi")
                        .version("1.0.0"));
    }
}
