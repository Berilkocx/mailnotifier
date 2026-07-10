package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.MailExpectationService;
import com.beril.mailnotifier.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ExpectationsPageController {

    private final MailExpectationService expectationService;
    private final NotificationService notificationService;
    private final SecurityHelper securityHelper;

    @GetMapping("/expectations")
    public String expectations(Authentication auth, Model model) {
        User user = securityHelper.getCurrentUser(auth);
        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("expectations", expectationService.getAllExpectations(user));
        model.addAttribute("activePage", "expectations");
        return "expectations";
    }
}
