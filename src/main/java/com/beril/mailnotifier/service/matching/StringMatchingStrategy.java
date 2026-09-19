package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StringMatchingStrategy implements MatchingStrategy {

    private static final Locale TURKISH = Locale.of("tr", "TR");

    @Override
    public MatchResult match(MailMessage mail, MailExpectation expectation) {
        boolean hasSender = expectation.getSenderIdentifier() != null
                && !expectation.getSenderIdentifier().isBlank();
        boolean hasKeywords = expectation.getKeywords() != null
                && !expectation.getKeywords().isEmpty();

        if (!hasSender && !hasKeywords) {
            return MatchResult.noMatch();
        }

        List<String> matchedTokens = new ArrayList<>();
        List<String> matchedKeywords = new ArrayList<>();

        double senderScore = hasSender
                ? calculateSenderScore(mail, expectation.getSenderIdentifier(), matchedTokens)
                : 0.0;
        double keywordScore = hasKeywords
                ? calculateKeywordScore(mail, expectation.getKeywords(), matchedKeywords)
                : 0.0;

        double totalScore;
        if (hasSender && hasKeywords) {
            totalScore = senderScore * 0.4 + keywordScore * 0.6;
        } else if (hasSender) {
            totalScore = senderScore;
        } else {
            totalScore = keywordScore;
        }

        if (totalScore < 0.15) {
            return MatchResult.noMatch();
        }

        ConfidenceLevel level = resolveConfidence(totalScore);
        String summary = buildSummary(matchedTokens, matchedKeywords, mail.fromEmail());

        return new MatchResult(true, totalScore, matchedKeywords, matchedTokens, level, summary, null);
    }

    private double calculateSenderScore(MailMessage mail, String senderIdentifier, List<String> matchedTokens) {
        List<String> tokens = SenderTokenizer.tokenize(senderIdentifier);
        if (tokens.isEmpty()) return 0.0;

        String effectiveContent = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
        int matched = 0;
        for (String token : tokens) {
            if (SenderTokenizer.fieldContains(mail.from(), token)
                    || SenderTokenizer.fieldContains(mail.fromEmail(), token)
                    || SenderTokenizer.fieldContains(mail.subject(), token)
                    || SenderTokenizer.fieldContains(effectiveContent, token)) {
                matchedTokens.add(token);
                matched++;
            }
        }
        return (double) matched / tokens.size();
    }

    private double calculateKeywordScore(MailMessage mail, List<String> keywords, List<String> matched) {
        String effectiveContent = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
        int matchCount = 0;
        for (String keyword : keywords) {
            String k = keyword.toLowerCase(TURKISH);
            if (SenderTokenizer.fieldContains(mail.subject(), k) || SenderTokenizer.fieldContains(effectiveContent, k)) {
                matched.add(keyword);
                matchCount++;
            }
        }
        return (double) matchCount / keywords.size();
    }

    public ConfidenceLevel resolveConfidence(double score) {
        if (score >= 0.7) return ConfidenceLevel.HIGH;
        if (score >= 0.4) return ConfidenceLevel.MEDIUM;
        return ConfidenceLevel.LOW;
    }

    private String buildSummary(List<String> matchedTokens, List<String> matchedKeywords, String fromEmail) {
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
              .append("' kelimesi bulundu.");
        }
        return sb.toString();
    }
}
