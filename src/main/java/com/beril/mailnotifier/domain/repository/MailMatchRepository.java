package com.beril.mailnotifier.domain.repository;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MailMatchRepository extends JpaRepository<MailMatch, UUID> {

    Optional<MailMatch> findByGmailMessageId(String gmailMessageId);

    boolean existsByGmailMessageIdAndExpectation_Id(String gmailMessageId, UUID expectationId);

    List<MailMatch> findByExpectation_IdOrderByCreatedAtDesc(UUID expectationId);

    List<MailMatch> findByExpectation_User_IdAndDismissedFalseOrderByCreatedAtDesc(UUID userId);

    List<MailMatch> findByExpectation_User_IdAndConfidenceLevelAndDismissedFalseOrderByCreatedAtDesc(
            UUID userId, ConfidenceLevel confidenceLevel);

    Optional<MailMatch> findByIdAndExpectation_User_Id(UUID id, UUID userId);

    long countByExpectation_IdAndDismissedFalse(UUID expectationId);

    long countByExpectation_User_IdAndDismissedFalse(UUID userId);

    long countByExpectation_User_IdAndDismissedFalseAndCreatedAtAfter(UUID userId, java.time.LocalDateTime after);

    List<MailMatch> findTop5ByExpectation_User_IdAndDismissedFalseOrderByCreatedAtDesc(UUID userId);
}
