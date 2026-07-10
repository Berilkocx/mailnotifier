package com.beril.mailnotifier.service.matching;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.service.ai.AiAnalysisResult;

import java.util.List;

public record MatchResult(
        boolean matched,
        double score,
        List<String> matchedKeywords,
        List<String> matchedSenderTokens,
        ConfidenceLevel confidenceLevel,
        String matchSummary,
        AiAnalysisResult aiResult
) {
    public static MatchResult noMatch() {
        return new MatchResult(false, 0.0, List.of(), List.of(), null, null, null);
    }
}
