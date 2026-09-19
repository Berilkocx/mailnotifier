package com.beril.mailnotifier.config;

import com.beril.mailnotifier.service.ai.AiRateLimiter;
import com.beril.mailnotifier.service.ai.AiService;
import com.beril.mailnotifier.service.ai.NoOpAiService;
import com.beril.mailnotifier.service.ai.OpenAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class AiConfig {

    @Bean
    public AiService aiService(
            @Value("${mailnotifier.ai.api-key:}") String apiKey,
            AiRateLimiter rateLimiter,
            ObjectMapper objectMapper) {

        if (apiKey == null || apiKey.isBlank()) {
            log.info("AI_API_KEY tanımlı değil — AI analizi devre dışı, yerel NLP ile devam ediliyor");
            return new NoOpAiService();
        }

        log.info("AI servisi başlatıldı (gpt-4o-mini)");
        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        return new OpenAiService(restClient, objectMapper, rateLimiter);
    }
}
