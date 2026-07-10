package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.MailExpectationService;
import com.beril.mailnotifier.service.MailMatchService;
import com.beril.mailnotifier.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final SecurityHelper securityHelper;
    private final MailExpectationService expectationService;
    private final MailMatchService matchService;
    private final NotificationService notificationService;

    @GetMapping("/")
    public String index(Authentication auth) {
        return (auth != null && auth.isAuthenticated()) ? "redirect:/dashboard" : "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) return "redirect:/dashboard";
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = securityHelper.getCurrentUser(auth);
        long unreadCount = notificationService.getUnreadCount(user);
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("activeExpectationCount", expectationService.countActiveByUser(user));
        model.addAttribute("totalMatchCount", matchService.countByUser(user));
        model.addAttribute("unreadNotificationCount", unreadCount);
        model.addAttribute("todayMatchCount", matchService.countTodayByUser(user));
        model.addAttribute("recentMatches", matchService.getRecentMatches(user));
        model.addAttribute("recentNotifications", notificationService.getRecentNotifications(user));
        model.addAttribute("activePage", "dashboard");
        return "dashboard";
    }
}
