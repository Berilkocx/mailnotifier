package com.beril.mailnotifier.web;

import com.beril.mailnotifier.dto.ApiResponse;
import com.beril.mailnotifier.service.ai.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "AI", description = "AI destekli keyword önerisi")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @Operation(summary = "Beklenti açıklamasına göre keyword öner")
    @PostMapping("/suggest-keywords")
    public ResponseEntity<ApiResponse<Map<String, List<String>>>> suggestKeywords(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String description = request.getOrDefault("description", "");
        List<String> suggested = aiService.suggestKeywords(description);
        return ResponseEntity.ok(ApiResponse.success(Map.of("suggestedKeywords", suggested)));
    }
}
