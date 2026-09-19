package com.beril.mailnotifier.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenEncryptionServiceTest {

    private static final String VALID_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final TokenEncryptionService service = new TokenEncryptionService(VALID_KEY);

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String token = "ya29.a0Af-secret-access-token";

        String encrypted = service.encrypt(token);

        assertThat(encrypted).isNotEqualTo(token);
        assertThat(service.decrypt(encrypted)).isEqualTo(token);
    }

    @Test
    void encrypt_producesDifferentCiphertextEachTime() {
        String token = "same-plaintext";

        String first = service.encrypt(token);
        String second = service.encrypt(token);

        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo(token);
        assertThat(service.decrypt(second)).isEqualTo(token);
    }

    @Test
    void encryptAndDecrypt_returnNull_whenInputIsNull() {
        assertThat(service.encrypt(null)).isNull();
        assertThat(service.decrypt(null)).isNull();
    }

    @Test
    void constructor_rejectsKeyWithWrongLength() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new TokenEncryptionService(shortKey))
                .isInstanceOf(IllegalStateException.class);
    }
}
