package com.beril.mailnotifier.security;

import com.beril.mailnotifier.domain.entity.AuthProvider;
import com.beril.mailnotifier.service.UserService;
import com.beril.mailnotifier.util.TokenEncryptionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomOAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final UserService userService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User principal = oauthToken.getPrincipal();

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");
        String picture = principal.getAttribute("picture");
        String sub = principal.getAttribute("sub");

        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        String encodedAccess = TokenEncryptionUtil.encode(client.getAccessToken().getTokenValue());
        Instant expiresAt = client.getAccessToken().getExpiresAt();

        String encodedRefresh = null;
        if (client.getRefreshToken() != null) {
            encodedRefresh = TokenEncryptionUtil.encode(client.getRefreshToken().getTokenValue());
        } else {
            log.warn("Refresh token gelmedi — email={}", email);
        }

        userService.saveOrUpdateUser(email, name, picture, sub, AuthProvider.GOOGLE,
                encodedAccess, encodedRefresh, expiresAt);

        log.info("Kullanıcı giriş yaptı: email={}", email);
        auditLog.info("LOGIN email={} provider=GOOGLE", email);
        response.sendRedirect("/dashboard");
    }
}
