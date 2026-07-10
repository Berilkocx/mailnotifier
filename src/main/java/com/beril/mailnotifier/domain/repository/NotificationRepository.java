package com.beril.mailnotifier.domain.repository;

import com.beril.mailnotifier.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_IdOrderBySentAtDesc(UUID userId);

    List<Notification> findByUser_IdAndIsReadFalse(UUID userId);

    long countByUser_IdAndIsReadFalse(UUID userId);

    Optional<Notification> findByIdAndUser_Id(UUID id, UUID userId);

    List<Notification> findTop5ByUser_IdOrderBySentAtDesc(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    void markAllReadByUserId(@Param("userId") UUID userId);
}
