package com.beril.mailnotifier.mail.gmail;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.exception.GmailApiException;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.mail.MailProvider;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

@Slf4j
@Component("gmailProvider")
@RequiredArgsConstructor
public class GmailMailProvider implements MailProvider {

    private static final String APPLICATION_NAME = "MailNotifier";

    private final TokenRefreshService tokenRefreshService;

    @Override
    public List<MailMessage> fetchNewMessages(User user, Instant since) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        try {
            Gmail gmail = buildGmailService(accessToken);
            String query = "after:" + since.getEpochSecond();

            ListMessagesResponse listResponse = gmail.users().messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults(100L)
                    .execute();

            if (listResponse.getMessages() == null) {
                return List.of();
            }

            List<MailMessage> messages = new ArrayList<>();
            for (Message stub : listResponse.getMessages()) {
                try {
                    Message msg = gmail.users().messages()
                            .get("me", stub.getId())
                            .setFormat("FULL")
                            .execute();
                    messages.add(toMailMessage(msg));
                } catch (IOException e) {
                    log.warn("Mesaj alınamadı id={}: {}", stub.getId(), e.getMessage());
                }
            }
            return messages;

        } catch (GoogleJsonResponseException e) {
            throw translateGoogleError(e);
        } catch (GeneralSecurityException | IOException e) {
            throw new GmailApiException("Gmail API çağrısı başarısız: " + e.getMessage(), e);
        }
    }

    @Override
    public MailMessage getMessage(User user, String messageId) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        try {
            Gmail gmail = buildGmailService(accessToken);
            Message msg = gmail.users().messages()
                    .get("me", messageId)
                    .setFormat("FULL")
                    .execute();
            return toMailMessage(msg);
        } catch (GoogleJsonResponseException e) {
            throw translateGoogleError(e);
        } catch (GeneralSecurityException | IOException e) {
            throw new GmailApiException("Mesaj alınamadı id=" + messageId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean testConnection(User user) {
        String accessToken = tokenRefreshService.getValidAccessToken(user);
        try {
            Gmail gmail = buildGmailService(accessToken);
            gmail.users().getProfile("me").execute();
            return true;
        } catch (Exception e) {
            log.error("Gmail bağlantı testi başarısız: {}", e.getMessage());
            return false;
        }
    }

    private Gmail buildGmailService(String accessToken) throws GeneralSecurityException, IOException {
        AccessToken token = new AccessToken(accessToken, Date.from(Instant.now().plusSeconds(3600)));
        GoogleCredentials credentials = GoogleCredentials.create(token);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        ).setApplicationName(APPLICATION_NAME).build();
    }

    private GmailApiException translateGoogleError(GoogleJsonResponseException e) {
        return switch (e.getStatusCode()) {
            case 401 -> {
                log.error("Gmail 401 - token geçersiz: {}", e.getMessage());
                yield new GmailApiException("Gmail erişim tokenı geçersiz. Lütfen yeniden giriş yapın.", e);
            }
            case 403 -> {
                log.error("Gmail 403 - yetersiz izin: {}", e.getMessage());
                yield new GmailApiException("Gmail izni yetersiz. Lütfen uygulamayı yeniden yetkilendirin.", e);
            }
            case 429 -> {
                log.error("Gmail 429 - rate limit aşıldı: {}", e.getMessage());
                yield new GmailApiException("Gmail API istek limiti aşıldı. Lütfen daha sonra tekrar deneyin.", e);
            }
            default -> new GmailApiException("Gmail API hatası (" + e.getStatusCode() + "): " + e.getMessage(), e);
        };
    }

    private MailMessage toMailMessage(Message msg) {
        String from = header(msg, "From");
        return new MailMessage(
                msg.getId(),
                extractName(from),
                extractEmail(from),
                header(msg, "Subject"),
                msg.getSnippet(),
                extractBody(msg.getPayload()),
                msg.getInternalDate() != null
                        ? Instant.ofEpochMilli(msg.getInternalDate())
                        : Instant.now()
        );
    }

    private String extractBody(MessagePart payload) {
        if (payload == null) return null;

        // Prefer text/plain; fall back to text/html
        String plain = findPartBody(payload, "text/plain");
        if (plain != null && !plain.isBlank()) return plain;

        String html = findPartBody(payload, "text/html");
        if (html != null && !html.isBlank()) return html;

        return null;
    }

    private String findPartBody(MessagePart part, String mimeType) {
        if (part == null) return null;

        if (mimeType.equals(part.getMimeType())) {
            return decodeBase64(part.getBody());
        }

        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                String result = findPartBody(child, mimeType);
                if (result != null && !result.isBlank()) return result;
            }
        }
        return null;
    }

    private String decodeBase64(com.google.api.services.gmail.model.MessagePartBody body) {
        if (body == null || body.getData() == null) return null;
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(body.getData());
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("Base64 decode başarısız: {}", e.getMessage());
            return null;
        }
    }

    private String header(Message msg, String name) {
        if (msg.getPayload() == null || msg.getPayload().getHeaders() == null) return null;
        return msg.getPayload().getHeaders().stream()
                .filter(h -> name.equalsIgnoreCase(h.getName()))
                .findFirst()
                .map(MessagePartHeader::getValue)
                .orElse(null);
    }

    private String extractEmail(String from) {
        if (from == null) return null;
        int s = from.indexOf('<'), e = from.indexOf('>');
        return (s >= 0 && e > s) ? from.substring(s + 1, e).trim() : from.trim();
    }

    private String extractName(String from) {
        if (from == null) return null;
        int bracket = from.indexOf('<');
        return bracket > 0 ? from.substring(0, bracket).trim() : from.trim();
    }
}
