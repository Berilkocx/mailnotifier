package com.beril.mailnotifier.service.ai;

import java.util.List;

public record AiAnalysisResult(
        String intent,
        List<String> topics,
        String summary,
        double confidence,
        double relevance,
        String reason
) {
    public static final double RELEVANCE_UNKNOWN = -1.0;

    public AiAnalysisResult(String intent, List<String> topics, String summary, double confidence) {
        this(intent, topics, summary, confidence, RELEVANCE_UNKNOWN, null);
    }

    public static AiAnalysisResult empty() {
        return new AiAnalysisResult("GENEL", List.of(), null, 0.0);
    }

    /** AI, mailin beklentiyle ne kadar ilgili olduğunu (0-1) söyleyebildi mi? */
    public boolean hasRelevance() {
        return relevance >= 0.0;
    }

    public boolean isEmpty() {
        return (topics == null || topics.isEmpty()) && confidence == 0.0 && !hasRelevance();
    }
}
