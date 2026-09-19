package com.beril.mailnotifier.service.ai;

import java.util.List;

public class NoOpAiService implements AiService {

    @Override
    public AiAnalysisResult analyzeEmail(String from, String subject, String content, ExpectationContext expectation) {
        return AiAnalysisResult.empty();
    }

    @Override
    public List<String> suggestKeywords(String description) {
        return List.of();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
