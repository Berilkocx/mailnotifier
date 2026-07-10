package com.beril.mailnotifier.security;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private final UserService userService;

    public User getCurrentUser(Authentication auth) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) auth;
        String email = token.getPrincipal().getAttribute("email");
        return userService.findByEmail(email);
    }
}
