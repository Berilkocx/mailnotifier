package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.ConfidenceLevel;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.ApiResponse;
import com.beril.mailnotifier.dto.MailMatchResponse;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.MailMatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Eşleşmeler", description = "Mail eşleşme sorgulama ve yönetimi")
@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MailMatchController {

    private final MailMatchService matchService;
    private final SecurityHelper securityHelper;

    @Operation(summary = "Eşleşmeleri listele", description = "Opsiyonel olarak confidence seviyesine göre filtreler")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MailMatchResponse>>> list(
            @RequestParam(required = false) ConfidenceLevel confidence,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(matchService.getMatches(user, confidence)));
    }

    @Operation(summary = "Eşleşme detayını getir")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MailMatchResponse>> get(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(matchService.getMatch(user, id)));
    }

    @Operation(summary = "Beklentiye ait eşleşmeleri listele")
    @GetMapping("/by-expectation/{expectationId}")
    public ResponseEntity<ApiResponse<List<MailMatchResponse>>> byExpectation(
            @PathVariable UUID expectationId,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(matchService.getMatchesByExpectation(user, expectationId)));
    }

    @Operation(summary = "Eşleşmeyi sil")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        matchService.deleteMatch(user, id);
        return ResponseEntity.ok(ApiResponse.success("Eşleşme silindi.", null));
    }
}
