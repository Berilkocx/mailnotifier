package com.beril.mailnotifier.service.ai;

import java.util.List;

public interface AiService {
    AiAnalysisResult analyzeEmail(String from, String subject, String snippet);
    List<String> suggestKeywords(String description);
    boolean isAvailable();
}
