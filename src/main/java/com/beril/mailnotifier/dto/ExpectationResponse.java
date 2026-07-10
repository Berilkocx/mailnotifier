package com.beril.mailnotifier.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExpectationResponse(
        UUID id,
        String senderIdentifier,
        List<String> keywords,
        String description,
        boolean isActive,
        long matchCount,
        LocalDateTime createdAt,
        LocalDateTime matchedAt
) {}
