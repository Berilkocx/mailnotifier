package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class MatchesPageController {

    private final MailMatchService matchService;
    private final MailExpectationService expectationService;
    private final NotificationService notificationService;
    private final SecurityHelper securityHelper;

    @GetMapping("/matches")
    public String matches(
            @RequestParam(required = false) String confidence,
            @RequestParam(required = false) String expectationId,
            @RequestParam(required = false, defaultValue = "all") String period,
            Authentication auth, Model model) {

        User user = securityHelper.getCurrentUser(auth);

        ConfidenceLevel level = null;
        if (confidence != null && !confidence.isBlank()) {
            try { level = ConfidenceLevel.valueOf(confidence.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        UUID expId = null;
        if (expectationId != null && !expectationId.isBlank()) {
            try { expId = UUID.fromString(expectationId); }
            catch (IllegalArgumentException ignored) {}
        }

        model.addAttribute("user", user);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(user));
        model.addAttribute("matches", matchService.getFilteredMatches(user, level, expId, period));
        model.addAttribute("expectations", expectationService.getAllExpectations(user));
        model.addAttribute("selectedConfidence", confidence);
        model.addAttribute("selectedExpectation", expectationId);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("activePage", "matches");
        return "matches";
    }
}
