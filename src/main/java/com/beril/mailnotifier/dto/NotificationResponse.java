package com.beril.mailnotifier.dto;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String message,
        ConfidenceLevel confidenceLevel,
        String expectationDescription,
        String mailFrom,
        String mailSubject,
        double matchScore,
        boolean isRead,
        LocalDateTime createdAt
) {}
