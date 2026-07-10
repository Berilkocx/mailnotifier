package com.beril.mailnotifier.dto;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MailMatchResponse(
        UUID id,
        UUID expectationId,
        String expectationDescription,
        String fromAddress,
        String fromEmail,
        String subject,
        String snippet,
        double matchScore,
        List<String> matchedKeywords,
        List<String> matchedSenderTokens,
        ConfidenceLevel confidenceLevel,
        String matchSummary,
        boolean dismissed,
        LocalDateTime receivedAt,
        LocalDateTime createdAt,
        String contentSummary,
        String detectedIntent,
        List<String> aiTopics
) {}
