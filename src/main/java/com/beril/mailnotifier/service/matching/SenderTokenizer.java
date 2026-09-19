package com.beril.mailnotifier.service.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gönderen kriterini aranabilir parçalara böler.
 * "Ahmet Yılmaz" → ["ahmet", "yılmaz"]
 * "ahmet@firma.com" → ["ahmet@firma.com", "ahmet", "firma.com", "firma"]
 */
public final class SenderTokenizer {

    private static final Locale TURKISH = Locale.of("tr", "TR");
    private static final int MIN_TOKEN_LENGTH = 3;

    private SenderTokenizer() {}

    public static List<String> tokenize(String senderIdentifier) {
        if (senderIdentifier == null || senderIdentifier.isBlank()) return List.of();

        List<String> tokens = new ArrayList<>();
        for (String part : senderIdentifier.trim().split("\\s+")) {
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

    public static boolean fieldContains(String field, String token) {
        if (field == null || field.isBlank()) return false;
        return field.toLowerCase(TURKISH).contains(token);
    }
}
