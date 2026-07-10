package com.beril.mailnotifier.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class OpenAiService implements AiService {

    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TOKENS = 500;
    private static final int SNIPPET_LIMIT = 1000;

    private static final String SYSTEM_PROMPT =
            "Sen bir mail analiz asistanısın. Verilen mailin içeriğini Türkçe olarak analiz et ve " +
            "JSON formatında yanıt ver. Yanıtın kesinlikle geçerli JSON olmalı, başka metin ekleme.";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AiRateLimiter rateLimiter;

    public OpenAiService(RestClient restClient, ObjectMapper objectMapper, AiRateLimiter rateLimiter) {
        this.restClient   = restClient;
        this.objectMapper = objectMapper;
        this.rateLimiter  = rateLimiter;
    }

    @Override
    public AiAnalysisResult analyzeEmail(String from, String subject, String snippet) {
        if (!rateLimiter.tryAcquire()) {
            return AiAnalysisResult.empty();
        }
        String truncatedSnippet = snippet != null && snippet.length() > SNIPPET_LIMIT
                ? snippet.substring(0, SNIPPET_LIMIT) : snippet;

        String userPrompt = String.format("""
                Gönderen: %s
                Konu: %s
                İçerik: %s

                Mailin amacını, konularını ve kısa özetini JSON formatında döndür:
                {
                  "intent": "TEKLIF|ONAY|RET|BASVURU_CEVABI|GENEL",
                  "topics": ["konu1", "konu2"],
                  "summary": "Kısa özet (1-2 cümle)",
                  "confidence": 0.0-1.0
                }
                """, from, subject, truncatedSnippet);

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(buildRequest(userPrompt))
                    .retrieve()
                    .body(String.class);

            return parseAnalysisResponse(responseBody);
        } catch (Exception e) {
            log.warn("AI mail analizi başarısız: {}", e.getMessage());
            return AiAnalysisResult.empty();
        }
    }

    @Override
    public List<String> suggestKeywords(String description) {
        if (description == null || description.isBlank()) return List.of();
        if (!rateLimiter.tryAcquire()) return List.of();

        String userPrompt = String.format("""
                Kullanıcının aşağıdaki mail beklentisi için en uygun Türkçe anahtar kelimeleri öner.
                Beklenti: %s

                5-8 adet kısa anahtar kelime döndür (JSON string array):
                ["kelime1", "kelime2", ...]
                """, description);

        try {
            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(buildRequest(userPrompt))
                    .retrieve()
                    .body(String.class);

            return parseKeywordResponse(responseBody);
        } catch (Exception e) {
            log.warn("AI keyword önerisi başarısız: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private Map<String, Object> buildRequest(String userPrompt) {
        return Map.of(
                "model", MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "max_tokens", MAX_TOKENS,
                "temperature", 0.1
        );
    }

    private AiAnalysisResult parseAnalysisResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.at("/choices/0/message/content").asText();

            // ```json blokları varsa temizle
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            JsonNode parsed = objectMapper.readTree(content);

            String intent = parsed.path("intent").asText("GENEL");
            double confidence = parsed.path("confidence").asDouble(0.0);
            String summary = parsed.path("summary").asText(null);

            List<String> topics = new ArrayList<>();
            JsonNode topicsNode = parsed.path("topics");
            if (topicsNode.isArray()) {
                topicsNode.forEach(n -> topics.add(n.asText()));
            }

            return new AiAnalysisResult(intent, topics, summary, confidence);
        } catch (Exception e) {
            log.warn("AI yanıtı parse edilemedi: {}", e.getMessage());
            return AiAnalysisResult.empty();
        }
    }

    private List<String> parseKeywordResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.at("/choices/0/message/content").asText();
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            List<String> keywords = new ArrayList<>();
            JsonNode parsed = objectMapper.readTree(content);
            if (parsed.isArray()) {
                parsed.forEach(n -> keywords.add(n.asText()));
            }
            return keywords;
        } catch (Exception e) {
            log.warn("AI keyword yanıtı parse edilemedi: {}", e.getMessage());
            return List.of();
        }
    }
}
