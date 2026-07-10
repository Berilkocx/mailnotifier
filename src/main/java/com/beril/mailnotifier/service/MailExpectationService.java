package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.MailExpectationRepository;
import com.beril.mailnotifier.domain.repository.MailMatchRepository;
import com.beril.mailnotifier.dto.CreateExpectationRequest;
import com.beril.mailnotifier.dto.ExpectationResponse;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailExpectationService {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final MailExpectationRepository expectationRepository;
    private final MailMatchRepository matchRepository;

    public ExpectationResponse createExpectation(User user, CreateExpectationRequest request) {
        validateAtLeastOneCriteria(request);
        MailExpectation entity = MailExpectation.builder()
                .user(user)
                .senderIdentifier(request.getSenderIdentifier())
                .keywords(request.getKeywords())
                .description(request.getDescription())
                .isActive(true)
                .build();
        ExpectationResponse response = toResponse(expectationRepository.save(entity));
        auditLog.info("EXPECTATION_CREATED userId={} sender={}", user.getId(), request.getSenderIdentifier());
        return response;
    }

    public List<ExpectationResponse> getExpectations(User user) {
        return expectationRepository.findByUser_IdAndIsActiveTrue(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ExpectationResponse> getAllExpectations(User user) {
        return expectationRepository.findByUser_Id(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long countActiveByUser(User user) {
        return expectationRepository.countByUser_IdAndIsActiveTrue(user.getId());
    }

    public ExpectationResponse getExpectation(User user, UUID id) {
        return toResponse(findOwned(user, id));
    }

    public ExpectationResponse updateExpectation(User user, UUID id, CreateExpectationRequest request) {
        validateAtLeastOneCriteria(request);
        MailExpectation entity = findOwned(user, id);
        entity.setSenderIdentifier(request.getSenderIdentifier());
        entity.setKeywords(request.getKeywords());
        entity.setDescription(request.getDescription());
        return toResponse(expectationRepository.save(entity));
    }

    public void deleteExpectation(User user, UUID id) {
        MailExpectation entity = findOwned(user, id);
        entity.setIsActive(false);
        expectationRepository.save(entity);
        auditLog.info("EXPECTATION_DEACTIVATED userId={} expectationId={}", user.getId(), id);
    }

    public ExpectationResponse activateExpectation(User user, UUID id) {
        MailExpectation entity = findOwned(user, id);
        entity.setIsActive(true);
        return toResponse(expectationRepository.save(entity));
    }

    private MailExpectation findOwned(User user, UUID id) {
        return expectationRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Beklenti", "id", id));
    }

    private void validateAtLeastOneCriteria(CreateExpectationRequest request) {
        boolean noSender = request.getSenderIdentifier() == null || request.getSenderIdentifier().isBlank();
        boolean noKeywords = request.getKeywords() == null || request.getKeywords().isEmpty();
        if (noSender && noKeywords) {
            throw new IllegalArgumentException("En az bir kriter girilmeli: senderIdentifier veya keywords");
        }
    }

    private ExpectationResponse toResponse(MailExpectation e) {
        long count = matchRepository.countByExpectation_IdAndDismissedFalse(e.getId());
        return new ExpectationResponse(
                e.getId(),
                e.getSenderIdentifier(),
                e.getKeywords(),
                e.getDescription(),
                Boolean.TRUE.equals(e.getIsActive()),
                count,
                e.getCreatedAt(),
                e.getMatchedAt()
        );
    }
}
