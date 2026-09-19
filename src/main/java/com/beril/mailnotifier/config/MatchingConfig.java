package com.beril.mailnotifier.config;

import com.beril.mailnotifier.service.ai.AiService;
import com.beril.mailnotifier.service.matching.MatchingStrategy;
import com.beril.mailnotifier.service.matching.NlpMatchingStrategy;
import com.beril.mailnotifier.service.matching.SemanticMatchingStrategy;
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

        if ("string".equals(strategy)) {
            log.info("Aktif eşleşme stratejisi: String (klasik kelime eşleşmesi)");
            return new StringMatchingStrategy();
        }

        SemanticMatchingStrategy semanticStrategy = new SemanticMatchingStrategy();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Aktif eşleşme stratejisi: NLP (yerel). AI_API_KEY tanımlı olmadığı için " +
                    "AI ile anlamsal değerlendirme ve içerik özeti devre dışı.");
            return semanticStrategy;
        }

        log.info("Aktif eşleşme stratejisi: NLP (yerel NLP + AI anlamsal analiz)");
        return new NlpMatchingStrategy(aiService, semanticStrategy);
    }
}
