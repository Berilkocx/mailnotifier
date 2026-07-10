package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StringMatchingStrategyTest {

    private StringMatchingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new StringMatchingStrategy();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MailMessage mail(String from, String fromEmail, String subject, String snippet) {
        return new MailMessage("msg-1", from, fromEmail, subject, snippet, null, Instant.now());
    }

    private MailExpectation expectation(String sender, List<String> keywords) {
        return MailExpectation.builder()
                .senderIdentifier(sender)
                .keywords(keywords)
                .build();
    }

    // ── Sender eşleşme testleri ───────────────────────────────────────────

    @Test
    void tam_email_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Ahmet <ahmet@firma.com>", "ahmet@firma.com", "Konu", "");
        MatchResult result = strategy.match(m, expectation("ahmet@firma.com", null));

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedSenderTokens()).contains("ahmet@firma.com");
    }

    @Test
    void kismi_email_domain_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Destek <destek@firma.com>", "destek@firma.com", "Konu", "");
        MatchResult result = strategy.match(m, expectation("firma.com", null));

        assertThat(result.matched()).isTrue();
    }

    @Test
    void ad_soyad_token_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Ahmet Yılmaz", "ahmet@ornek.com", "", "");
        MatchResult result = strategy.match(m, expectation("Ahmet Yılmaz", null));

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedSenderTokens()).contains("ahmet", "yılmaz");
    }

    @Test
    void sadece_ad_token_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Ahmet Demir", "ahmet@ornek.com", "", "");
        MatchResult result = strategy.match(m, expectation("Ahmet", null));

        assertThat(result.matched()).isTrue();
    }

    @Test
    void kurum_adi_kismi_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Gelir İdaresi Başkanlığı <info@gib.gov.tr>", "info@gib.gov.tr", "Vergi bildirimi", "");
        MatchResult result = strategy.match(m, expectation("gelir idaresi", null));

        assertThat(result.matched()).isTrue();
    }

    // ── Keyword eşleşme testleri ──────────────────────────────────────────

    @Test
    void keyword_konuda_eslesmesi_case_insensitive_matched_true_donmeli() {
        MailMessage m = mail("Biri", "biri@ornek.com", "Yeni Teklif Geldi", "");
        MatchResult result = strategy.match(m, expectation(null, List.of("teklif")));

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedKeywords()).contains("teklif");
    }

    @Test
    void keyword_snippet_icinde_bulunmasi_matched_true_donmeli() {
        MailMessage m = mail("Biri", "biri@ornek.com", "Bilgi", "Teklifimizi incelemenizi rica ederiz.");
        MatchResult result = strategy.match(m, expectation(null, List.of("teklif")));

        assertThat(result.matched()).isTrue();
    }

    @Test
    void turkce_karakter_keyword_eslesmesi_matched_true_donmeli() {
        MailMessage m = mail("Okul", "okul@edu.tr", "Sınav sonuçları açıklandı", "");
        MatchResult result = strategy.match(m, expectation(null, List.of("sınav")));

        assertThat(result.matched()).isTrue();
    }

    // ── Skor ve ConfidenceLevel testleri ─────────────────────────────────

    @Test
    void hem_sender_hem_keyword_eslesmesi_agirlikli_skor_hesaplamali() {
        // sender %100 eşleşiyor (0.4 ağırlık) + keyword %100 (0.6 ağırlık) → skor 1.0
        MailMessage m = mail("Ahmet <ahmet@firma.com>", "ahmet@firma.com", "Teklif", "");
        MatchResult result = strategy.match(m, expectation("ahmet@firma.com", List.of("teklif")));

        assertThat(result.score()).isGreaterThanOrEqualTo(0.7);
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void yuksek_skor_HIGH_confidence_donmeli() {
        MailMessage m = mail("Ahmet Yılmaz <ahmet@firma.com>", "ahmet@firma.com", "Teklif Belgesi", "teklif ekte");
        MatchResult result = strategy.match(m,
                expectation("ahmet@firma.com", List.of("teklif", "belge")));

        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.HIGH);
    }

    @Test
    void orta_skor_MEDIUM_confidence_donmeli() {
        // Tek keyword'den bir tanesi eşleşiyor → skor 0.5 × 0.6 = 0.3? Hayır, sadece keyword varsa direkt keyword skoru.
        // 1 keyword'den 1'i eşleşiyor → skor = 1.0; HIGH olur.
        // Bunu MEDIUM yapmak için: 2 keyword'den 1'i eşleşsin → skor = 0.5; MEDIUM.
        MailMessage m = mail("X", "x@y.com", "Mülakat daveti", "");
        MatchResult result = strategy.match(m,
                expectation(null, List.of("mülakat", "teknik")));  // sadece "mülakat" eşleşiyor

        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.MEDIUM);
    }

    @Test
    void dusuk_skor_LOW_confidence_donmeli() {
        // sender 3 token'dan 1'i eşleşiyor → skor = 1/3 ≈ 0.33; ama tek sender varsa direkt sender skoru.
        // 3 token'dan 1 eşleşme = 0.33 → LOW
        MailMessage m = mail("Zeynep Demir", "zeynep@ornek.com", "Selam", "");
        // 3 token: "ahmet", "zeynep", "kaya" → sadece "zeynep" eşleşiyor
        MatchResult result = strategy.match(m,
                expectation("Ahmet Zeynep Kaya", null));

        assertThat(result.matched()).isTrue();
        assertThat(result.confidenceLevel()).isEqualTo(ConfidenceLevel.LOW);
    }

    // ── Eşleşme yok testleri ─────────────────────────────────────────────

    @Test
    void ilgisiz_mail_no_match_donmeli() {
        MailMessage m = mail("Spam <spam@spam.com>", "spam@spam.com", "Kazan kazan", "Büyük ödül sizi bekliyor");
        MatchResult result = strategy.match(m, expectation("ahmet@firma.com", List.of("teklif")));

        assertThat(result.matched()).isFalse();
        assertThat(result.score()).isEqualTo(0.0);
    }

    @Test
    void bos_kriter_no_match_donmeli() {
        MailMessage m = mail("Kim", "kim@ornek.com", "Konu", "İçerik");
        MatchResult result = strategy.match(m, expectation(null, null));

        assertThat(result.matched()).isFalse();
    }

    @Test
    void uc_karakterden_kisa_token_atlaniyor() {
        // "AB" sender'ı tokenize edildiğinde MIN_TOKEN_LENGTH=3 nedeniyle atlanır → eşleşme yok
        MailMessage m = mail("AB <ab@ab.com>", "ab@ab.com", "Konu", "");
        MatchResult result = strategy.match(m, expectation("AB", null));

        // "ab" 2 karakter — token olarak eklenmemeli; domain "ab.com" 6 karakter token olur
        // ama "ab.com" mailde yoksa eşleşme olmaz
        assertThat(result.matched()).isFalse();
    }

    @Test
    void match_summary_bos_degilse_matched_tokens_icerir() {
        MailMessage m = mail("Ahmet <ahmet@firma.com>", "ahmet@firma.com", "Teklif", "teklif mektubu");
        MatchResult result = strategy.match(m,
                expectation("ahmet@firma.com", List.of("teklif")));

        assertThat(result.matchSummary()).isNotBlank();
    }
}
