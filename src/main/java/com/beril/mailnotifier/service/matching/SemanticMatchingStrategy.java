package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mail içeriğini Türkçe NLP ile tarar: kelimeler köklerine indirgenir ve anlamdaşlarıyla
 * genişletilir. Böylece "fatura" beklentisi "faturanız" ve "ödeme hatırlatması" mailini de yakalar.
 * Harici bir AI servisine ihtiyaç duymaz.
 */
public class SemanticMatchingStrategy implements MatchingStrategy {

    private static final double MATCH_THRESHOLD = 0.15;

    @Override
    public MatchResult match(MailMessage mail, MailExpectation expectation) {
        boolean hasSender = expectation.getSenderIdentifier() != null
                && !expectation.getSenderIdentifier().isBlank();
        boolean hasKeywords = expectation.getKeywords() != null
                && !expectation.getKeywords().isEmpty();
        boolean hasDescription = expectation.getDescription() != null
                && !expectation.getDescription().isBlank();

        if (!hasSender && !hasKeywords && !hasDescription) {
            return MatchResult.noMatch();
        }

        Set<String> contentStems = contentStems(mail);

        List<String> matchedTokens = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        double senderScore = hasSender
                ? senderScore(mail, expectation.getSenderIdentifier(), matchedTokens)
                : 0.0;
        double keywordScore = hasKeywords
                ? keywordScore(expectation.getKeywords(), contentStems, matchedKeywords)
                : 0.0;
        double descriptionScore = hasDescription
                ? descriptionScore(expectation.getDescription(), contentStems)
                : 0.0;

        double contentScore;
        if (hasKeywords) {
            contentScore = keywordScore * 0.8 + descriptionScore * 0.2;
        } else {
            contentScore = descriptionScore;
        }

        double totalScore;
        if (hasSender && (hasKeywords || hasDescription)) {
            totalScore = senderScore * 0.4 + contentScore * 0.6;
        } else if (hasSender) {
            totalScore = senderScore;
        } else {
            totalScore = contentScore;
        }

        if (totalScore < MATCH_THRESHOLD) {
            return MatchResult.noMatch();
        }

        return new MatchResult(
                true,
                totalScore,
                matchedKeywords,
                matchedTokens,
                resolveConfidence(totalScore),
                buildSummary(matchedTokens, matchedKeywords, descriptionScore),
                null
        );
    }

    /** Konu ve gövde birlikte taranır; gövde yoksa snippet kullanılır. */
    private Set<String> contentStems(MailMessage mail) {
        String body = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
        Set<String> stems = new LinkedHashSet<>(TurkishTextAnalyzer.stemsOf(mail.subject()));
        stems.addAll(TurkishTextAnalyzer.stemsOf(body));
        return stems;
    }

    private double senderScore(MailMessage mail, String senderIdentifier, List<String> matchedTokens) {
        List<String> tokens = SenderTokenizer.tokenize(senderIdentifier);
        if (tokens.isEmpty()) return 0.0;

        String content = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
        int matched = 0;
        for (String token : tokens) {
            if (SenderTokenizer.fieldContains(mail.from(), token)
                    || SenderTokenizer.fieldContains(mail.fromEmail(), token)
                    || SenderTokenizer.fieldContains(mail.subject(), token)
                    || SenderTokenizer.fieldContains(content, token)) {
                matchedTokens.add(token);
                matched++;
            }
        }
        return (double) matched / tokens.size();
    }

    private double keywordScore(List<String> keywords, Set<String> contentStems, List<String> matched) {
        int matchCount = 0;
        for (String keyword : keywords) {
            if (TurkishTextAnalyzer.matches(keyword, contentStems)) {
                matched.add(keyword);
                matchCount++;
            }
        }
        return (double) matchCount / keywords.size();
    }

    /** Beklenti açıklamasındaki anlamlı kelimelerin mailde ne kadar karşılık bulduğu. */
    private double descriptionScore(String description, Set<String> contentStems) {
        Set<String> descriptionStems = TurkishTextAnalyzer.stemsOf(description);
        if (descriptionStems.isEmpty()) return 0.0;

        long matched = descriptionStems.stream()
                .filter(stem -> TurkishTextAnalyzer.matches(stem, contentStems))
                .count();
        return (double) matched / descriptionStems.size();
    }

    public ConfidenceLevel resolveConfidence(double score) {
        if (score >= 0.7) return ConfidenceLevel.HIGH;
        if (score >= 0.4) return ConfidenceLevel.MEDIUM;
        return ConfidenceLevel.LOW;
    }

    private String buildSummary(List<String> matchedTokens, List<String> matchedKeywords, double descriptionScore) {
        StringBuilder sb = new StringBuilder();

        if (!matchedTokens.isEmpty()) {
            sb.append("Gönderen alanında '")
              .append(String.join("', '", matchedTokens))
              .append("' eşleşti.");
        }
        if (!matchedKeywords.isEmpty()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("İçerikte '")
              .append(String.join("', '", matchedKeywords))
              .append("' ile ilgili ifadeler bulundu.");
        } else if (descriptionScore > 0) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append("Mail içeriği beklenti açıklamanızla örtüşüyor.");
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
