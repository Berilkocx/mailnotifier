package com.beril.mailnotifier.config;

import com.beril.mailnotifier.service.ai.AiService;
import com.beril.mailnotifier.service.matching.MatchingStrategy;
import com.beril.mailnotifier.service.matching.NlpMatchingStrategy;
import com.beril.mailnotifier.service.matching.StringMatchingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MatchingConfig {

    @Bean
    public MatchingStrategy matchingStrategy(
            AiService aiService,
            @Value("${mailnotifier.matching.strategy:string}") String strategy,
            @Value("${mailnotifier.ai.api-key:}") String apiKey) {

        StringMatchingStrategy stringStrategy = new StringMatchingStrategy();

        if ("nlp".equals(strategy)) {
            if (apiKey == null || apiKey.isBlank()) {
                log.warn("NLP strateji seçildi ancak AI_API_KEY tanımlı değil — string matching'e düşülüyor");
                return stringStrategy;
            }
            log.info("Aktif eşleşme stratejisi: NLP (AI destekli)");
            return new NlpMatchingStrategy(aiService, stringStrategy);
        }

        log.info("Aktif eşleşme stratejisi: String (klasik)");
        return stringStrategy;
    }
}
