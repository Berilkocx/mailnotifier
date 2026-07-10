package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.ApiResponse;
import com.beril.mailnotifier.dto.NotificationResponse;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Bildirimler", description = "Bildirim yönetimi")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityHelper securityHelper;

    @Operation(summary = "Tüm bildirimleri listele")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> list(Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(user)));
    }

    @Operation(summary = "Okunmamış bildirim sayısını getir")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(user)));
    }

    @Operation(summary = "Bildirimi okundu olarak işaretle")
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        notificationService.markAsRead(user, id);
        return ResponseEntity.ok(ApiResponse.success("Bildirim okundu olarak işaretlendi.", null));
    }

    @Operation(summary = "Tüm bildirimleri okundu olarak işaretle")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRead(Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("Tüm bildirimler okundu olarak işaretlendi.", null));
    }

    @Operation(summary = "Bildirimi sil")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        notificationService.deleteNotification(user, id);
        return ResponseEntity.ok(ApiResponse.success("Bildirim silindi.", null));
    }
}
