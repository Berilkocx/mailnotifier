package com.beril.mailnotifier.service.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Türkçe metin analizi: normalizasyon, kök bulma ve eş anlamlı genişletme.
 * Harici servise ihtiyaç duymadan "faturanız" ≈ "fatura" ≈ "ödeme" eşleşmesini sağlar.
 */
public final class TurkishTextAnalyzer {

    private static final Locale TURKISH = Locale.of("tr", "TR");

    /** Kök en az bu kadar karakter kalacaksa ek atılır. */
    private static final int MIN_STEM_LENGTH = 3;

    private static final Set<String> STOP_WORDS = Set.of(
            "ve", "veya", "ile", "bir", "bu", "şu", "da", "de", "ki", "mi", "mı", "mu", "mü",
            "için", "gibi", "ama", "fakat", "çok", "daha", "en", "her", "hiç", "ne", "var", "yok",
            "olarak", "olan", "sonra", "önce", "kadar", "ise", "ya", "ancak", "tüm", "bazı",
            "kendi", "diye", "göre", "üzere", "ben", "sen", "biz", "siz", "onlar", "bana", "size",
            "sayın", "merhaba", "lütfen", "bilgi", "konu", "mail", "eposta", "mesaj"
    );

    /** Uzundan kısaya denenen yapım/çekim ekleri. */
    private static final List<String> SUFFIXES = List.of(
            "larımız", "lerimiz", "larınız", "leriniz", "larında", "lerinde",
            "ınızı", "inizi", "unuzu", "ünüzü", "ınız", "iniz", "unuz", "ünüz",
            "larda", "lerde", "lardan", "lerden", "ları", "leri", "lar", "ler",
            "nızı", "nizi", "nuzu", "nüzü", "nız", "niz", "nuz", "nüz",
            "mızı", "mizi", "muzu", "müzü", "mız", "miz", "muz", "müz",
            "dan", "den", "tan", "ten", "nın", "nin", "nun", "nün",
            "lık", "lik", "luk", "lük", "sal", "sel",
            "da", "de", "ta", "te", "ya", "ye", "sı", "si", "su", "sü",
            "ın", "in", "un", "ün", "ım", "im", "um", "üm",
            "cı", "ci", "cu", "cü", "çı", "çi", "çu", "çü",
            "yı", "yi", "yu", "yü",
            "a", "e", "ı", "i", "u", "ü"
    );

    /** Aynı satırdaki kelimeler anlamca akraba sayılır. */
    private static final List<List<String>> SYNONYM_GROUPS = List.of(
            List.of("fatura", "ödeme", "borç", "tahsilat", "tutar", "ücret", "abonelik"),
            List.of("kargo", "gönderi", "teslimat", "sipariş", "paket", "teslim", "sevkiyat"),
            List.of("mülakat", "görüşme", "başvuru", "pozisyon", "aday", "işe alım", "kariyer", "cv"),
            List.of("teklif", "fiyat", "öneri", "proforma", "indirim", "kampanya"),
            List.of("onay", "kabul", "tebrik", "olumlu", "onaylandı"),
            List.of("ret", "red", "olumsuz", "reddedildi", "üzgünüz"),
            List.of("randevu", "toplantı", "davet", "buluşma"),
            List.of("şifre", "parola", "doğrulama", "kod", "güvenlik"),
            List.of("sözleşme", "imza", "anlaşma", "kontrat"),
            List.of("iptal", "iade", "değişiklik", "erteleme")
    );

    /** kök → aynı anlam grubundaki tüm kökler */
    private static final Map<String, Set<String>> SYNONYM_INDEX = buildSynonymIndex();

    private TurkishTextAnalyzer() {}

    private static Map<String, Set<String>> buildSynonymIndex() {
        Map<String, Set<String>> index = new HashMap<>();
        for (List<String> group : SYNONYM_GROUPS) {
            Set<String> groupStems = new HashSet<>();
            for (String word : group) {
                groupStems.addAll(stemsOf(word));
            }
            for (String stem : groupStems) {
                index.computeIfAbsent(stem, k -> new HashSet<>()).addAll(groupStems);
            }
        }
        return Map.copyOf(index);
    }

    /** Türkçe küçük harfe çevirir ve aksanlı harfleri sadeleştirir ("Ödeme" → "odeme"). */
    public static String normalize(String text) {
        if (text == null) return "";
        String lower = text.toLowerCase(TURKISH);
        StringBuilder sb = new StringBuilder(lower.length());
        for (char c : lower.toCharArray()) {
            sb.append(switch (c) {
                case 'ı' -> 'i';
                case 'ş' -> 's';
                case 'ğ' -> 'g';
                case 'ü' -> 'u';
                case 'ö' -> 'o';
                case 'ç' -> 'c';
                case 'â' -> 'a';
                case 'î' -> 'i';
                case 'û' -> 'u';
                default -> c;
            });
        }
        return sb.toString();
    }

    /** Ekleri kırparak kaba bir kök üretir. Dilbilimsel doğruluktan çok tutarlılık hedeflenir. */
    public static String stem(String word) {
        String current = normalize(word);
        for (int pass = 0; pass < 3; pass++) {
            String stripped = stripOneSuffix(current);
            if (stripped.equals(current)) break;
            current = stripped;
        }
        return current;
    }

    private static String stripOneSuffix(String word) {
        for (String suffix : SUFFIXES) {
            String normalizedSuffix = normalize(suffix);
            if (word.length() - normalizedSuffix.length() >= MIN_STEM_LENGTH
                    && word.endsWith(normalizedSuffix)) {
                return word.substring(0, word.length() - normalizedSuffix.length());
            }
        }
        return word;
    }

    /** Metni anlamlı kelime köklerine ayırır; durak kelimeler ve sayılar elenir. */
    public static Set<String> stemsOf(String text) {
        Set<String> stems = new LinkedHashSet<>();
        for (String token : normalize(text).split("[^a-z0-9@.]+")) {
            if (token.isBlank() || token.length() < 2) continue;
            if (STOP_WORDS.contains(token)) continue;
            if (token.chars().allMatch(Character::isDigit)) continue;
            stems.add(stem(token));
        }
        return stems;
    }

    /** Bir anahtar kelimenin kökü ve anlamdaşlarının kökleri. */
    public static Set<String> expand(String keyword) {
        Set<String> forms = new LinkedHashSet<>(stemsOf(keyword));
        Set<String> expanded = new LinkedHashSet<>(forms);
        for (String form : forms) {
            expanded.addAll(SYNONYM_INDEX.getOrDefault(form, Set.of()));
        }
        return expanded;
    }

    /**
     * Anahtar kelime metinde anlamca geçiyor mu?
     * Çok kelimeli ifadelerde ("son ödeme") tüm parçalar aranır.
     */
    public static boolean matches(String keyword, Set<String> textStems) {
        if (textStems.isEmpty()) return false;

        List<String> parts = new ArrayList<>(stemsOf(keyword));
        if (parts.isEmpty()) return false;

        if (parts.size() > 1) {
            return parts.stream().allMatch(part -> containsStem(part, textStems));
        }
        return expand(keyword).stream().anyMatch(form -> containsStem(form, textStems));
    }

    /** Kök eşitliği; bileşik yazımlar için tek yönlü içerme de kabul edilir. */
    private static boolean containsStem(String stem, Set<String> textStems) {
        if (textStems.contains(stem)) return true;
        if (stem.length() < 4) return false;
        return textStems.stream().anyMatch(t -> t.length() >= 4 && (t.startsWith(stem) || stem.startsWith(t)));
    }
}
