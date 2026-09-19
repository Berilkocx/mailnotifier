package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.NotificationResponse;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationsPageController.class)
class NotificationsPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SecurityHelper securityHelper;

    @Test
    @WithMockUser
    void notificationsPage_rendersDateAndSenderEmail() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).email("test@example.com").name("Test").build();
        NotificationResponse notification = new NotificationResponse(
                UUID.randomUUID(), "Mail geldi", ConfidenceLevel.HIGH, "Fatura maili",
                "Ahmet Yılmaz", "ahmet@firma.com", "Fatura", 0.9, false,
                LocalDateTime.of(2026, 9, 16, 14, 33));

        when(securityHelper.getCurrentUser(any())).thenReturn(user);
        when(notificationService.getUnreadCount(user)).thenReturn(1L);
        when(notificationService.getNotifications(user)).thenReturn(List.of(notification));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("16.09.2026 14:33")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ahmet Yılmaz")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;ahmet@firma.com&gt;")));
    }
}
