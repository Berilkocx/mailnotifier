package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.*;
import com.beril.mailnotifier.domain.repository.NotificationRepository;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test Kullanıcı")
                .build();
    }

    private MailMatch buildMatch(ConfidenceLevel level) {
        MailExpectation expectation = MailExpectation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .description("Test beklentisi")
                .build();

        return MailMatch.builder()
                .id(UUID.randomUUID())
                .expectation(expectation)
                .gmailMessageId("msg-123")
                .fromAddress("Gönderen <gonderen@ornek.com>")
                .fromEmail("gonderen@ornek.com")
                .subject("Test Konusu")
                .matchScore(0.8)
                .confidenceLevel(level)
                .dismissed(false)
                .build();
    }

    // ── createNotification ────────────────────────────────────────────────

    @Test
    void createNotification_kaydeder_ve_websocket_gonderir() {
        MailMatch match = buildMatch(ConfidenceLevel.HIGH);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("📬 Beklediğiniz mail gelmiş görünüyor!")
                .confidenceLevel(ConfidenceLevel.HIGH)
                .isRead(false)
                .build();

        when(notificationRepository.save(any())).thenReturn(saved);

        service.createNotification(testUser, match);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications/" + testUser.getId()),
                any(Object.class)
        );
    }

    @Test
    void createNotification_HIGH_confidence_dogru_mesaj_formati() {
        MailMatch match = buildMatch(ConfidenceLevel.HIGH);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("📬 Beklediğiniz mail gelmiş görünüyor!")
                .confidenceLevel(ConfidenceLevel.HIGH)
                .isRead(false)
                .build();

        when(notificationRepository.save(captor.capture())).thenReturn(saved);

        service.createNotification(testUser, match);

        String message = captor.getValue().getMessage();
        assertThat(message).contains("📬").contains("Beklediğiniz mail gelmiş görünüyor");
    }

    @Test
    void createNotification_MEDIUM_confidence_dogru_mesaj_formati() {
        MailMatch match = buildMatch(ConfidenceLevel.MEDIUM);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("📩 Beklediğiniz maile benzer bir mail gelmiş olabilir.")
                .confidenceLevel(ConfidenceLevel.MEDIUM)
                .isRead(false)
                .build();

        when(notificationRepository.save(captor.capture())).thenReturn(saved);

        service.createNotification(testUser, match);

        String message = captor.getValue().getMessage();
        assertThat(message).contains("📩").contains("benzer");
    }

    @Test
    void createNotification_LOW_confidence_dogru_mesaj_formati() {
        MailMatch match = buildMatch(ConfidenceLevel.LOW);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("💡 Bu mail beklediğinizle ilgili olabilir.")
                .confidenceLevel(ConfidenceLevel.LOW)
                .isRead(false)
                .build();

        when(notificationRepository.save(captor.capture())).thenReturn(saved);

        service.createNotification(testUser, match);

        String message = captor.getValue().getMessage();
        assertThat(message).contains("💡");
    }

    // ── markAsRead ────────────────────────────────────────────────────────

    @Test
    void markAsRead_isRead_true_yapilmali() {
        UUID notifId = UUID.randomUUID();
        MailMatch match = buildMatch(ConfidenceLevel.HIGH);

        Notification notification = Notification.builder()
                .id(notifId)
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("test")
                .isRead(false)
                .build();

        when(notificationRepository.findByIdAndUser_Id(notifId, testUser.getId()))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        service.markAsRead(testUser, notifId);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_baska_kullanici_bildirimi_exception_firlatmali() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findByIdAndUser_Id(notifId, testUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(testUser, notifId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getUnreadCount ────────────────────────────────────────────────────

    @Test
    void getUnreadCount_repository_degerini_donmeli() {
        when(notificationRepository.countByUser_IdAndIsReadFalse(testUser.getId())).thenReturn(5L);

        long count = service.getUnreadCount(testUser);

        assertThat(count).isEqualTo(5L);
    }

    // ── WebSocket hatası sessizce yutulmalı ───────────────────────────────

    @Test
    void createNotification_websocket_hatasi_exception_firlatmamali() {
        MailMatch match = buildMatch(ConfidenceLevel.HIGH);

        Notification saved = Notification.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message("test")
                .confidenceLevel(ConfidenceLevel.HIGH)
                .isRead(false)
                .build();

        when(notificationRepository.save(any())).thenReturn(saved);
        doThrow(new RuntimeException("WebSocket hatası")).when(messagingTemplate)
                .convertAndSend(anyString(), any(Object.class));

        // WebSocket hata fırlatsa bile createNotification başarıyla tamamlanmalı
        service.createNotification(testUser, match);

        verify(notificationRepository).save(any());
    }
}
