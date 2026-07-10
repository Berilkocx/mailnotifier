package com.beril.mailnotifier.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LogoutController {

    // Spring Security varsayılan olarak yalnızca POST /logout'u işler.
    // Tarayıcı adres çubuğundan doğrudan erişilen GET /logout istekleri bu endpoint tarafından karşılanır.
    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/login?logout";
    }
}
