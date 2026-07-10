package com.beril.mailnotifier.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Phase 2: Base64 encoding as placeholder — will be replaced with AES-256 in Phase 6
public final class TokenEncryptionUtil {

    private TokenEncryptionUtil() {}

    public static String encode(String token) {
        if (token == null) return null;
        return Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    public static String decode(String encodedToken) {
        if (encodedToken == null) return null;
        return new String(Base64.getDecoder().decode(encodedToken), StandardCharsets.UTF_8);
    }
}
