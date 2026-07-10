package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.ApiResponse;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.mail.MailProvider;
import com.beril.mailnotifier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
public class GmailController {

    @Qualifier("gmailProvider")
    private final MailProvider mailProvider;

    private final UserService userService;

    @GetMapping("/test-connection")
    public ResponseEntity<ApiResponse<Boolean>> testConnection(Authentication auth) {
        User user = resolveUser(auth);
        boolean ok = mailProvider.testConnection(user);
        return ResponseEntity.ok(ApiResponse.success("Bağlantı durumu", ok));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<MailMessage>>> recent(
            @RequestParam(defaultValue = "10") int count,
            Authentication auth) {
        User user = resolveUser(auth);
        // Son 30 günlük mesajlar arasından count kadar getir
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        List<MailMessage> messages = mailProvider.fetchNewMessages(user, since)
                .stream()
                .limit(count)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    private User resolveUser(Authentication auth) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        String email = token.getPrincipal().getAttribute("email");
        return userService.findByEmail(email);
    }
}
