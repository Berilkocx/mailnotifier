package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.domain.entity.MailMatch;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.MailExpectationRepository;
import com.beril.mailnotifier.domain.repository.MailMatchRepository;
import com.beril.mailnotifier.domain.repository.UserRepository;
import com.beril.mailnotifier.exception.GmailApiException;
import com.beril.mailnotifier.mail.MailMessage;
import com.beril.mailnotifier.mail.MailProvider;
import com.beril.mailnotifier.service.matching.MatchResult;
import com.beril.mailnotifier.service.matching.MatchingStrategy;
import com.beril.mailnotifier.service.ai.AiRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "mailnotifier.scan.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MailScanService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Qualifier("gmailProvider")
    private final MailProvider mailProvider;

    private final UserRepository userRepository;
    private final MailExpectationRepository expectationRepository;
    private final MailMatchRepository matchRepository;
    private final MatchingStrategy matchingStrategy;
    private final NotificationService notificationService;
    private final AiRateLimiter aiRateLimiter;

    @Scheduled(fixedRateString = "${mailnotifier.scan.interval:60000}")
    public void scan() {
        log.debug("Mail taraması başlıyor...");
        List<User> users = userRepository.findAllByRefreshTokenIsNotNull();
        for (User user : users) {
            try {
                scanForUser(user);
            } catch (Exception e) {
                log.error("Kullanıcı taranırken hata oluştu: userId={}, hata={}", user.getId(), e.getMessage());
            }
        }
    }

    private void scanForUser(User user) {
        aiRateLimiter.resetScanCount();
        List<MailExpectation> expectations = expectationRepository.findByUser_IdAndIsActiveTrue(user.getId());
        if (expectations.isEmpty()) {
            return;
        }

        Instant since = user.getLastScanAt() != null
                ? user.getLastScanAt()
                : Instant.now().minus(24, ChronoUnit.HOURS);

        List<MailMessage> messages;
        try {
            messages = mailProvider.fetchNewMessages(user, since);
        } catch (GmailApiException e) {
            if (e.getMessage() != null && e.getMessage().contains("limit")) {
                log.warn("Gmail rate limit: userId={}", user.getId());
            } else {
                log.error("Gmail API hatası: userId={}, hata={}", user.getId(), e.getMessage());
            }
            return;
        }

        int saved = 0;
        for (MailMessage mail : messages) {
            for (MailExpectation expectation : expectations) {
                if (matchRepository.existsByGmailMessageIdAndExpectation_Id(mail.messageId(), expectation.getId())) {
                    continue;
                }
                MatchResult result = matchingStrategy.match(mail, expectation);
                if (!result.matched()) continue;

                String contentSummary  = result.aiResult() != null ? result.aiResult().summary() : null;
                String detectedIntent  = result.aiResult() != null ? result.aiResult().intent() : null;
                java.util.List<String> aiTopics = result.aiResult() != null
                        ? result.aiResult().topics() : java.util.List.of();

                MailMatch match = MailMatch.builder()
                        .expectation(expectation)
                        .gmailMessageId(mail.messageId())
                        .fromAddress(mail.from())
                        .fromEmail(mail.fromEmail())
                        .subject(mail.subject())
                        .snippet(mail.snippet())
                        .matchScore(result.score())
                        .matchedKeywords(result.matchedKeywords())
                        .matchedSenderTokens(result.matchedSenderTokens())
                        .confidenceLevel(result.confidenceLevel())
                        .matchSummary(result.matchSummary())
                        .contentSummary(contentSummary)
                        .detectedIntent(detectedIntent)
                        .aiTopics(aiTopics)
                        .receivedAt(LocalDateTime.ofInstant(mail.receivedAt(), ZoneOffset.UTC))
                        .dismissed(false)
                        .build();
                MailMatch savedMatch = matchRepository.save(match);
                notificationService.createNotification(user, savedMatch);

                expectation.setMatchedAt(LocalDateTime.now());
                expectationRepository.save(expectation);
                saved++;
            }
        }

        log.debug("Tarama tamamlandı: userId={}, mesaj={}, yeni eşleşme={}", user.getId(), messages.size(), saved);
        if (saved > 0) {
            auditLog.info("SCAN_MATCH userId={} newMatches={}", user.getId(), saved);
        }

        user.setLastScanAt(Instant.now());
        userRepository.save(user);
    }
}
