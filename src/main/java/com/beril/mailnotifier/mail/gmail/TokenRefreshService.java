package com.beril.mailnotifier.mail.gmail;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.UserRepository;
import com.beril.mailnotifier.exception.GmailApiException;
import com.beril.mailnotifier.util.TokenEncryptionUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;

    /**
     * Geçerli access token döner; süresi dolmuşsa refresh token ile yeniler.
     */
    public String getValidAccessToken(User user) {
        // 60 saniyelik tolerans: token yakında dolacaksa şimdiden yenile
        if (user.getTokenExpiresAt() != null &&
                Instant.now().isBefore(user.getTokenExpiresAt().minusSeconds(60))) {
            return TokenEncryptionUtil.decode(user.getAccessToken());
        }
        return refresh(user);
    }

    @Transactional
    public String refresh(User user) {
        if (user.getRefreshToken() == null) {
            throw new GmailApiException("Refresh token bulunamadı. Lütfen yeniden giriş yapın.");
        }

        try {
            String decodedRefresh = TokenEncryptionUtil.decode(user.getRefreshToken());
            GoogleTokenResponse response = new GoogleRefreshTokenRequest(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    decodedRefresh,
                    clientId,
                    clientSecret
            ).execute();

            String newAccessToken = response.getAccessToken();
            Instant newExpiresAt = Instant.now().plusSeconds(response.getExpiresInSeconds());

            user.setAccessToken(TokenEncryptionUtil.encode(newAccessToken));
            user.setTokenExpiresAt(newExpiresAt);
            userRepository.save(user);

            log.info("Access token yenilendi: userId={}", user.getId());
            return newAccessToken;

        } catch (IOException e) {
            log.error("Token yenileme başarısız: {}", e.getMessage());
            throw new GmailApiException("Access token yenilenemedi. Lütfen yeniden giriş yapın.", e);
        }
    }
}
