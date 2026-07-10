package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class NotificationsPageController {

    private final NotificationService notificationService;
    private final SecurityHelper securityHelper;

    @GetMapping("/notifications")
    public String notifications(Authentication auth, Model model) {
        User user = securityHelper.getCurrentUser(auth);
        long unreadCount = notificationService.getUnreadCount(user);
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("notifications", notificationService.getNotifications(user));
        model.addAttribute("activePage", "notifications");
        return "notifications";
    }
}
