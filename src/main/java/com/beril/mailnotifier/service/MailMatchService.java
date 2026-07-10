package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.MailMatch;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.MailExpectationRepository;
import com.beril.mailnotifier.domain.repository.MailMatchRepository;
import com.beril.mailnotifier.dto.MailMatchResponse;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailMatchService {

    private final MailMatchRepository matchRepository;
    private final MailExpectationRepository expectationRepository;

    public List<MailMatchResponse> getMatches(User user, ConfidenceLevel confidence) {
        List<MailMatch> matches = confidence != null
                ? matchRepository.findByExpectation_User_IdAndConfidenceLevelAndDismissedFalseOrderByCreatedAtDesc(
                        user.getId(), confidence)
                : matchRepository.findByExpectation_User_IdAndDismissedFalseOrderByCreatedAtDesc(user.getId());
        return matches.stream().map(this::toResponse).toList();
    }

    public List<MailMatchResponse> getFilteredMatches(User user, ConfidenceLevel confidence, UUID expectationId, String period) {
        if (expectationId != null && !expectationRepository.existsByIdAndUser_Id(expectationId, user.getId())) {
            throw new ResourceNotFoundException("Beklenti", "id", expectationId);
        }
        List<MailMatch> all = expectationId != null
                ? matchRepository.findByExpectation_IdOrderByCreatedAtDesc(expectationId)
                : matchRepository.findByExpectation_User_IdAndDismissedFalseOrderByCreatedAtDesc(user.getId());
        LocalDateTime cutoff = periodCutoff(period);
        return all.stream()
                .filter(m -> confidence == null || m.getConfidenceLevel() == confidence)
                .filter(m -> cutoff == null || (m.getCreatedAt() != null && m.getCreatedAt().isAfter(cutoff)))
                .map(this::toResponse)
                .toList();
    }

    public long countByUser(User user) {
        return matchRepository.countByExpectation_User_IdAndDismissedFalse(user.getId());
    }

    public long countTodayByUser(User user) {
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        return matchRepository.countByExpectation_User_IdAndDismissedFalseAndCreatedAtAfter(user.getId(), today);
    }

    public List<MailMatchResponse> getRecentMatches(User user) {
        return matchRepository.findTop5ByExpectation_User_IdAndDismissedFalseOrderByCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).toList();
    }

    public MailMatchResponse getMatch(User user, UUID id) {
        MailMatch match = matchRepository.findByIdAndExpectation_User_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Eşleşme", "id", id));
        return toResponse(match);
    }

    public List<MailMatchResponse> getMatchesByExpectation(User user, UUID expectationId) {
        if (!expectationRepository.existsByIdAndUser_Id(expectationId, user.getId())) {
            throw new ResourceNotFoundException("Beklenti", "id", expectationId);
        }
        return matchRepository.findByExpectation_IdOrderByCreatedAtDesc(expectationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteMatch(User user, UUID id) {
        MailMatch match = matchRepository.findByIdAndExpectation_User_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Eşleşme", "id", id));
        matchRepository.delete(match);
    }

    private LocalDateTime periodCutoff(String period) {
        if (period == null) return null;
        return switch (period) {
            case "today" -> LocalDateTime.now().toLocalDate().atStartOfDay();
            case "7d"    -> LocalDateTime.now().minusDays(7);
            case "30d"   -> LocalDateTime.now().minusDays(30);
            default      -> null;
        };
    }

    private MailMatchResponse toResponse(MailMatch m) {
        return new MailMatchResponse(
                m.getId(),
                m.getExpectation().getId(),
                m.getExpectation().getDescription(),
                m.getFromAddress(),
                m.getFromEmail(),
                m.getSubject(),
                m.getSnippet(),
                m.getMatchScore() != null ? m.getMatchScore() : 0.0,
                m.getMatchedKeywords(),
                m.getMatchedSenderTokens(),
                m.getConfidenceLevel(),
                m.getMatchSummary(),
                Boolean.TRUE.equals(m.getDismissed()),
                m.getReceivedAt(),
                m.getCreatedAt(),
                m.getContentSummary(),
                m.getDetectedIntent(),
                m.getAiTopics()
        );
    }
}
