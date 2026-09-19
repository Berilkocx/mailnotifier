package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.*;
import com.beril.mailnotifier.domain.repository.NotificationRepository;
import com.beril.mailnotifier.dto.NotificationResponse;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Notification createNotification(User user, MailMatch match) {
        Notification notification = Notification.builder()
                .user(user)
                .mailMatch(match)
                .type(NotificationType.IN_APP)
                .message(buildMessage(match))
                .confidenceLevel(match.getConfidenceLevel())
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);

        NotificationResponse response = toResponse(saved);
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + user.getId(), response);
        } catch (Exception e) {
            log.warn("WebSocket bildirimi gönderilemedi: userId={}, hata={}", user.getId(), e.getMessage());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(User user) {
        return notificationRepository.findByUser_IdOrderBySentAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
    }

    @Transactional
    public void markAsRead(User user, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim", "id", notificationId));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllReadByUserId(user.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getRecentNotifications(User user) {
        return notificationRepository.findTop5ByUser_IdOrderBySentAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteNotification(User user, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUser_Id(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Bildirim", "id", notificationId));
        notificationRepository.delete(notification);
    }

    private String buildMessage(MailMatch match) {
        String summary = match.getMatchSummary() != null && !match.getMatchSummary().isBlank()
                ? " " + match.getMatchSummary()
                : "";
        return switch (match.getConfidenceLevel()) {
            case HIGH   -> "📬 Beklediğiniz mail gelmiş görünüyor!" + summary;
            case MEDIUM -> "📩 Beklediğiniz maile benzer bir mail gelmiş olabilir." + summary;
            case LOW    -> "💡 Bu mail beklediğinizle ilgili olabilir." + summary;
        };
    }

    private NotificationResponse toResponse(Notification n) {
        MailMatch match = n.getMailMatch();
        return new NotificationResponse(
                n.getId(),
                n.getMessage(),
                n.getConfidenceLevel(),
                match.getExpectation().getDescription(),
                match.getFromAddress(),
                match.getFromEmail(),
                match.getSubject(),
                match.getMatchScore() != null ? match.getMatchScore() : 0.0,
                Boolean.TRUE.equals(n.getIsRead()),
                n.getSentAt()
        );
    }
}
