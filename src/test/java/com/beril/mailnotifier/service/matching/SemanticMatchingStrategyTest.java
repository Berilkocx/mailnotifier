package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticMatchingStrategyTest {

    private final SemanticMatchingStrategy strategy = new SemanticMatchingStrategy();
    private final StringMatchingStrategy classic = new StringMatchingStrategy();

    private MailMessage mail(String subject, String body) {
        return new MailMessage("msg-1", "Enerjisa", "no-reply@enerjisa.com", subject, null, body, Instant.now());
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
    void match_findsKeywordInInflectedForm() {
        MailMessage msg = mail("Eylül dönemi faturanız hazır", "Faturanızı görüntüleyebilirsiniz.");
        MailExpectation exp = expectation(null, List.of("fatura"), null);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedKeywords()).containsExactly("fatura");
    }

    @Test
    void match_findsKeywordThroughSynonym_whereClassicMatchingFails() {
        MailMessage msg = mail("Ödeme hatırlatması", "Dönem borcunuzun son ödeme günü 25 Eylül.");
        MailExpectation exp = expectation(null, List.of("fatura"), null);

        assertThat(classic.match(msg, exp).matched()).isFalse();

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchSummary()).contains("fatura");
    }

    @Test
    void match_isInsensitiveToTurkishCharactersAndCase() {
        MailMessage msg = mail("ODEME BILGISI", "odeme yapilmistir");
        MailExpectation exp = expectation(null, List.of("ödeme"), null);

        assertThat(strategy.match(msg, exp).matched()).isTrue();
    }

    @Test
    void match_unrelatedMail_doesNotMatch() {
        MailMessage msg = mail("Haftalık bülten", "Bu hafta öne çıkan yazılar ve blog içerikleri.");
        MailExpectation exp = expectation(null, List.of("mülakat"), null);

        assertThat(strategy.match(msg, exp).matched()).isFalse();
    }

    @Test
    void match_descriptionOnlyExpectation_usesContentSimilarity() {
        MailMessage msg = mail("Mülakat daveti", "Başvurunuz için görüşme yapmak istiyoruz.");
        MailExpectation exp = expectation(null, List.of(), "mülakat daveti bekliyorum");

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
    }

    @Test
    void match_multiWordKeyword_requiresAllParts() {
        MailExpectation exp = expectation(null, List.of("son ödeme"), null);

        assertThat(strategy.match(mail("Son ödeme tarihi", "25 Eylül"), exp).matched()).isTrue();
        assertThat(strategy.match(mail("Yeni ürünler", "Kampanya son gün"), exp).matched()).isFalse();
    }

    @Test
    void match_senderCriterionStillWorks() {
        MailMessage msg = mail("Herhangi bir konu", "içerik");
        MailExpectation exp = expectation("enerjisa.com", List.of(), null);

        MatchResult result = strategy.match(msg, exp);

        assertThat(result.matched()).isTrue();
        assertThat(result.matchedSenderTokens()).contains("enerjisa.com");
    }

    @Test
    void match_withoutAnyCriteria_returnsNoMatch() {
        assertThat(strategy.match(mail("Konu", "içerik"), expectation(null, List.of(), null)).matched()).isFalse();
    }
}
