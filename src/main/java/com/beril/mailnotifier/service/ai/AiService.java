package com.beril.mailnotifier.service.ai;

import java.util.List;

public interface AiService {
    AiAnalysisResult analyzeEmail(String from, String subject, String content, ExpectationContext expectation);
    List<String> suggestKeywords(String description);
    boolean isAvailable();
}
