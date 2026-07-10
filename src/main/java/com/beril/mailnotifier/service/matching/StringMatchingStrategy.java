package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.mail.MailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StringMatchingStrategy implements MatchingStrategy {

    private static final Locale TURKISH = Locale.of("tr", "TR");
    private static final int MIN_TOKEN_LENGTH = 3;

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
        List<String> tokens = tokenize(senderIdentifier);
        if (tokens.isEmpty()) return 0.0;

        String effectiveContent = mail.body() != null && !mail.body().isBlank() ? mail.body() : mail.snippet();
        int matched = 0;
        for (String token : tokens) {
            if (fieldContains(mail.from(), token)
                    || fieldContains(mail.fromEmail(), token)
                    || fieldContains(mail.subject(), token)
                    || fieldContains(effectiveContent, token)) {
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
            if (fieldContains(mail.subject(), k) || fieldContains(effectiveContent, k)) {
                matched.add(keyword);
                matchCount++;
            }
        }
        return (double) matchCount / keywords.size();
    }

    /**
     * senderIdentifier'ı token'lara ayırır.
     * "Ahmet Yılmaz" → ["ahmet", "yılmaz"]
     * "ahmet@firma.com" → ["ahmet@firma.com", "ahmet", "firma.com", "firma"]
     */
    private List<String> tokenize(String senderIdentifier) {
        List<String> tokens = new ArrayList<>();
        String[] parts = senderIdentifier.trim().split("\\s+");

        for (String part : parts) {
            String lower = part.toLowerCase(TURKISH);
            if (lower.length() >= MIN_TOKEN_LENGTH) {
                tokens.add(lower);
            }
            if (part.contains("@")) {
                String[] emailParts = part.split("@", 2);
                String local = emailParts[0].toLowerCase(TURKISH);
                if (local.length() >= MIN_TOKEN_LENGTH) tokens.add(local);

                if (emailParts.length == 2) {
                    String domain = emailParts[1].toLowerCase(TURKISH);
                    if (domain.length() >= MIN_TOKEN_LENGTH) tokens.add(domain);
                    int dot = domain.lastIndexOf('.');
                    if (dot > 0) {
                        String domainBase = domain.substring(0, dot);
                        if (domainBase.length() >= MIN_TOKEN_LENGTH) tokens.add(domainBase);
                    }
                }
            }
        }
        return tokens.stream().distinct().toList();
    }

    private boolean fieldContains(String field, String token) {
        if (field == null || field.isBlank()) return false;
        return field.toLowerCase(TURKISH).contains(token);
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
