package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.service.ai.AiAnalysisResult;
import com.beril.mailnotifier.service.ai.AiService;
import com.beril.mailnotifier.service.ai.ExpectationContext;
import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NlpMatchingStrategyTest {

    @Mock
    private AiService aiService;

    private NlpMatchingStrategy strategy;
    private SemanticMatchingStrategy semanticStrategy;

    @BeforeEach
    void setUp() {
        semanticStrategy = new SemanticMatchingStrategy();
        strategy = new NlpMatchingStrategy(aiService, semanticStrategy);
    }

    private MailMessage mail(String from, String subject, String snippet) {
        return new MailMessage("msg-1", from, from, subject, snippet, null, Instant.now());
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

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(aiResult);

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

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any()))
                .thenReturn(AiAnalysisResult.empty());

        MatchResult nlpResult = strategy.match(msg, exp);
        MatchResult semanticResult = semanticStrategy.match(msg, exp);

        assertThat(nlpResult.matched()).isEqualTo(semanticResult.matched());
    }

    @Test
    void match_whenAIThrows_fallsBackToStringResult() {
        MailMessage msg = mail("bilgi@firma.com", "Bilgi", "genel bilgi amaçlı");
        MailExpectation exp = expectation("firma.com", List.of("bilgi"), null);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AI servisi erişilemez"));

        assertThat(strategy.match(msg, exp)).isNotNull();
    }

    @Test
    void match_whenCombinedScoreBelowThreshold_returnsNoMatch() {
        MailMessage msg = mail("noreply@spam.com", "Reklam Fırsatı", "bu bir reklam iletisidir tıklayın");
        MailExpectation exp = expectation(null, List.of("teklif"), "iş teklifi");
        AiAnalysisResult lowConfidence = new AiAnalysisResult("GENEL", List.of(), null, 0.05);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(lowConfidence);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void match_topicKeywordOverlap_boostsScore() {
        MailMessage msg = mail("ik@sirket.com", "Mülakat Daveti", "başvurunuzu değerlendirdik");
        MailExpectation exp = expectation(null, List.of("mülakat", "başvuru"), "iş başvurusu cevabı bekliyorum");
        AiAnalysisResult aiResult = new AiAnalysisResult("BASVURU_CEVABI",
                List.of("mülakat", "işe alım"), "Mülakat daveti gönderildi", 0.8);

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(aiResult);

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

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(aiResult);

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

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(lowAi);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isFalse();
    }

    @Test
    void match_semanticallyRelevantMail_isCaughtWhereClassicStringMatchingFails() {
        MailMessage msg = mail("no-reply@enerjisa.com", "Ödeme hatırlatması", "Dönem borcunuzun son ödeme günü 25 Eylül");
        MailExpectation exp = expectation(null, List.of("fatura"), "elektrik faturamı bekliyorum");
        AiAnalysisResult ai = new AiAnalysisResult("GENEL", List.of("ödeme"), "Son ödeme günü 25 Eylül", 0.9,
                0.9, "Elektrik dönem borcu bildirimi, beklenen fatura ile aynı konu");

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(ai);

        assertThat(new StringMatchingStrategy().match(msg, exp).matched()).isFalse();

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        // Yerel NLP "ödeme"yi "fatura" ile anlamdaş sayar, AI da ilgili bulur → iki katman da doğruluyor
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
        assertThat(result.matchSummary()).contains("AI değerlendirmesi").contains("beklenen fatura");
    }

    @Test
    void match_keywordCoincidenceJudgedUnrelatedByAi_dropsToLowConfidence() {
        MailMessage msg = mail("kampanya@market.com", "Büyük indirim", "yeni fatura ödeme kampanyası ile %20 indirim");
        MailExpectation exp = expectation(null, List.of("fatura"), "elektrik faturamı bekliyorum");
        AiAnalysisResult ai = new AiAnalysisResult("GENEL", List.of("kampanya"), "Market kampanya duyurusu", 0.9,
                0.05, "Reklam maili, beklenen fatura değil");

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any())).thenReturn(ai);

        assertThat(new StringMatchingStrategy().match(msg, exp).confidenceLevel()).isNotEqualTo(ConfidenceLevel.LOW);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
    }

    @Test
    void match_passesUserExpectationToAi() {
        MailMessage msg = mail("ik@sirket.com", "Mülakat", "yarın saat 10:00");
        MailExpectation exp = expectation("sirket.com", List.of("mülakat"), "mülakat daveti bekliyorum");

        when(aiService.analyzeEmail(anyString(), anyString(), anyString(), any()))
                .thenReturn(AiAnalysisResult.empty());

        strategy.match(msg, exp);

        ArgumentCaptor<ExpectationContext> captor = ArgumentCaptor.forClass(ExpectationContext.class);
        verify(aiService).analyzeEmail(anyString(), anyString(), anyString(), captor.capture());
        assertThat(captor.getValue().description()).isEqualTo("mülakat daveti bekliyorum");
        assertThat(captor.getValue().senderIdentifier()).isEqualTo("sirket.com");
        assertThat(captor.getValue().keywords()).containsExactly("mülakat");
    }
}
