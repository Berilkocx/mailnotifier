package com.beril.mailnotifier.service.ai;

import java.util.List;

public record AiAnalysisResult(
        String intent,
        List<String> topics,
        String summary,
        double confidence
) {
    public static AiAnalysisResult empty() {
        return new AiAnalysisResult("GENEL", List.of(), null, 0.0);
    }

    public boolean isEmpty() {
        return (topics == null || topics.isEmpty()) && confidence == 0.0;
    }
}
