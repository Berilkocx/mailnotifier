package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.service.ai.AiAnalysisResult;
import com.beril.mailnotifier.service.ai.AiService;
import com.beril.mailnotifier.service.ai.ExpectationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class NlpMatchingStrategy implements MatchingStrategy {

    private static final Locale TURKISH = Locale.of("tr", "TR");

    private static final Map<String, List<String>> INTENT_KEYWORDS = Map.of(
            "TEKLIF",         List.of("teklif", "fiyat", "öneri", "quote"),
            "ONAY",           List.of("onay", "kabul", "onaylandı", "tebrik"),
            "RET",            List.of("ret", "reddedildi", "kabul edilmedi", "üzgünüz"),
            "BASVURU_CEVABI", List.of("başvuru", "iş başvurusu", "pozisyon", "mülakat"),
            "GENEL",          List.of()
    );

    private final AiService aiService;
    private final SemanticMatchingStrategy semanticStrategy;

    @Override
    public MatchResult match(MailMessage mail, MailExpectation expectation) {
        // 1. Yerel NLP taraması (kök + eş anlam)
        MatchResult stringResult = semanticStrategy.match(mail, expectation);

        // 2. AI analizi
        AiAnalysisResult aiResult = null;
        try {
            String aiContent = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
            aiResult = aiService.analyzeEmail(mail.from(), mail.subject(), aiContent,
                    new ExpectationContext(expectation.getDescription(),
                            expectation.getSenderIdentifier(), expectation.getKeywords()));
        } catch (Exception e) {
            log.warn("AI analizi sırasında hata: {}", e.getMessage());
        }

        // 3. AI başarısız veya boş ise sadece string skorunu kullan
        if (aiResult == null || aiResult.isEmpty()) {
            if (!stringResult.matched()) return MatchResult.noMatch();
            return new MatchResult(
                    stringResult.matched(),
                    stringResult.score(),
                    stringResult.matchedKeywords(),
                    stringResult.matchedSenderTokens(),
                    stringResult.confidenceLevel(),
                    stringResult.matchSummary(),
                    null
            );
        }

        // 4. Birleşik skor hesapla
        double stringScore = stringResult.matched() ? stringResult.score() : 0.0;
        double combinedScore = combineScores(stringScore, aiResult, expectation);

        log.debug("NLP skor: string={} ai={} → toplam={}",
                String.format("%.2f", stringScore),
                aiResult.hasRelevance() ? String.format("%.2f", aiResult.relevance()) : "n/a",
                String.format("%.2f", combinedScore));

        if (combinedScore < 0.15) {
            return MatchResult.noMatch();
        }

        ConfidenceLevel level = semanticStrategy.resolveConfidence(combinedScore);

        String summary = buildCombinedSummary(stringResult, aiResult);

        return new MatchResult(
                true,
                combinedScore,
                stringResult.matchedKeywords(),
                stringResult.matchedSenderTokens(),
                level,
                summary,
                aiResult
        );
    }

    /**
     * AI, mailin beklentiyle anlamsal ilgisini verdiyse ağırlık ona verilir: kelime tesadüfü tek başına
     * yüksek skor getirmez, kelime geçmeyen ama anlamca ilgili mail ise yakalanır.
     * İlgi skoru yoksa eski konu/niyet sezgisine düşülür.
     */
    private double combineScores(double stringScore, AiAnalysisResult aiResult, MailExpectation expectation) {
        if (aiResult.hasRelevance()) {
            return aiResult.relevance() * 0.7 + stringScore * 0.3;
        }
        double aiTopicScore  = calculateTopicScore(aiResult.topics(), expectation.getKeywords());
        double aiIntentScore = calculateIntentScore(aiResult.intent(), expectation.getDescription());
        return stringScore * 0.4
                + aiTopicScore * 0.3
                + aiIntentScore * 0.2
                + aiResult.confidence() * 0.1;
    }

    /** Jaccard-benzeri konu–keyword örtüşmesi. */
    private double calculateTopicScore(List<String> aiTopics, List<String> keywords) {
        if (aiTopics == null || aiTopics.isEmpty()) return 0.0;
        if (keywords == null || keywords.isEmpty()) return 0.0;

        int matched = 0;
        for (String topic : aiTopics) {
            String topicLower = topic.toLowerCase(TURKISH);
            Set<String> topicWords = Set.of(topicLower.split("\\s+"));
            for (String keyword : keywords) {
                String kwLower = keyword.toLowerCase(TURKISH);
                // topic kelimelerinden biri keyword'ü içeriyor mu veya keyword topic'i içeriyor mu?
                boolean overlap = topicLower.contains(kwLower)
                        || kwLower.contains(topicLower)
                        || topicWords.stream().anyMatch(w -> w.contains(kwLower) || kwLower.contains(w));
                if (overlap) {
                    matched++;
                    break;
                }
            }
        }
        return (double) matched / aiTopics.size();
    }

    /** AI intent ile beklenti açıklaması arasındaki uyum. */
    private double calculateIntentScore(String aiIntent, String expectationDescription) {
        if (aiIntent == null || "GENEL".equals(aiIntent)) return 0.05;
        if (expectationDescription == null || expectationDescription.isBlank()) return 0.1;

        List<String> intentKeywords = INTENT_KEYWORDS.getOrDefault(aiIntent, List.of());
        if (intentKeywords.isEmpty()) return 0.05;

        String descLower = expectationDescription.toLowerCase(TURKISH);
        long matchedCount = intentKeywords.stream()
                .filter(kw -> descLower.contains(kw.toLowerCase(TURKISH)))
                .count();

        return (double) matchedCount / intentKeywords.size();
    }

    private String buildCombinedSummary(MatchResult stringResult, AiAnalysisResult aiResult) {
        StringBuilder sb = new StringBuilder();
        if (stringResult.matchSummary() != null && !stringResult.matchSummary().isBlank()) {
            sb.append(stringResult.matchSummary());
        }
        // Özetin kendisi AI analiz kartında gösterilir; burada tekrar etmek yerine gerekçe eklenir
        if (aiResult.reason() != null && !aiResult.reason().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("AI değerlendirmesi: ").append(aiResult.reason());
        } else if (aiResult.summary() != null && !aiResult.summary().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("AI özeti: ").append(aiResult.summary());
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
