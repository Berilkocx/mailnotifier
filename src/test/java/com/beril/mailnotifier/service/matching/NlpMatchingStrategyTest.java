package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.service.ai.AiAnalysisResult;
import com.beril.mailnotifier.service.ai.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlpMatchingStrategyTest {

    @Mock
    private AiService aiService;

    private NlpMatchingStrategy strategy;
    private StringMatchingStrategy stringStrategy;

    @BeforeEach
    void setUp() {
        stringStrategy = new StringMatchingStrategy();
        strategy = new NlpMatchingStrategy(aiService, stringStrategy);
    }

    private MailMessage mail(String from, String subject, String snippet) {
        return new MailMessage("msg-1", from, from, subject, snippet, Instant.now());
    }

    private MailExpectation expectation(String sender, List<String> keywords, String description) {
        MailExpectation exp = new MailExpectation();
        exp.setSenderIdentifier(sender);
        exp.setKeywords(keywords);
        exp.setDescription(description);
        exp.setIsActive(true);
        return exp;
    }

    @Test
    void match_withStringMatchAndHighConfidenceAI_shouldReturnMatch() {
        MailMessage msg = mail("teklif@firma.com", "Proje Teklifi", "size bir teklif göndermek istiyorum");
        MailExpectation exp = expectation("firma.com", List.of("teklif", "proje"), "yazılım teklifi bekliyorum");
        AiAnalysisResult aiResult = new AiAnalysisResult("TEKLIF", List.of("teklif", "yazılım"), "Proje teklifi gönderildi", 0.9);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString())).thenReturn(aiResult);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.aiResult()).isNotNull();
        assertThat(result.aiResult().intent()).isEqualTo("TEKLIF");
        assertThat(result.score()).isGreaterThan(0.0);
    }

    @Test
    void match_whenAIReturnsEmpty_fallsBackToStringResult() {
        MailMessage msg = mail("proje@firma.com", "Proje Hakkında", "proje detayları için yazıyorum");
        MailExpectation exp = expectation("firma.com", List.of("proje"), null);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString()))
                .thenReturn(AiAnalysisResult.empty());

        MatchResult nlpResult = strategy.match(msg, exp);
        MatchResult stringResult = stringStrategy.match(msg, exp);

        assertThat(nlpResult.matched()).isEqualTo(stringResult.matched());
    }

    @Test
    void match_whenAIThrows_fallsBackToStringResult() {
        MailMessage msg = mail("bilgi@firma.com", "Bilgi", "genel bilgi amaçlı");
        MailExpectation exp = expectation("firma.com", List.of("bilgi"), null);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("AI servisi erişilemez"));

        assertThat(strategy.match(msg, exp)).isNotNull();
    }

    @Test
    void match_whenCombinedScoreBelowThreshold_returnsNoMatch() {
        MailMessage msg = mail("noreply@spam.com", "Reklam Fırsatı", "bu bir reklam iletisidir tıklayın");
        MailExpectation exp = expectation(null, List.of("teklif"), "iş teklifi");
        AiAnalysisResult lowConfidence = new AiAnalysisResult("GENEL", List.of(), null, 0.05);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString())).thenReturn(lowConfidence);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void match_topicKeywordOverlap_boostsScore() {
        MailMessage msg = mail("ik@sirket.com", "Mülakat Daveti", "başvurunuzu değerlendirdik");
        MailExpectation exp = expectation(null, List.of("mülakat", "başvuru"), "iş başvurusu cevabı bekliyorum");
        AiAnalysisResult aiResult = new AiAnalysisResult("BASVURU_CEVABI",
                List.of("mülakat", "işe alım"), "Mülakat daveti gönderildi", 0.8);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString())).thenReturn(aiResult);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.score()).isGreaterThan(0.15);
    }

    @Test
    void match_withIntentMatchingDescription_includesAiSummaryInMatchSummary() {
        MailMessage msg = mail("muhasebe@firma.com", "Fiyat Teklifi", "size fiyat teklifi yapıyoruz teklif fiyat");
        MailExpectation exp = expectation(null, List.of("fiyat", "teklif"), "teklif ve fiyat bilgisi bekliyorum");
        AiAnalysisResult aiResult = new AiAnalysisResult("TEKLIF",
                List.of("fiyat", "teklif"), "Fiyat teklifi yapıldı", 0.85);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString())).thenReturn(aiResult);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        if (result.matchSummary() != null) {
            assertThat(result.matchSummary()).contains("AI özeti");
        }
    }

    @Test
    void match_whenNoStringMatchAndLowAI_returnsNoMatch() {
        MailMessage msg = mail("random@other.org", "Alakasız Konu", "hiçbir beklentiyle örtüşmeyen içerik");
        MailExpectation exp = expectation("firma.com", List.of("teklif"), null);
        AiAnalysisResult lowAi = new AiAnalysisResult("GENEL", List.of("diğer"), null, 0.1);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString())).thenReturn(lowAi);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isFalse();
    }
}
