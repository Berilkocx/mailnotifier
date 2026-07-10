package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.ApiResponse;
import com.beril.mailnotifier.dto.CreateExpectationRequest;
import com.beril.mailnotifier.dto.ExpectationResponse;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.MailExpectationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Beklentiler", description = "Mail beklenti yönetimi")
@RestController
@RequestMapping("/api/expectations")
@RequiredArgsConstructor
public class MailExpectationController {

    private final MailExpectationService expectationService;
    private final SecurityHelper securityHelper;

    @Operation(summary = "Yeni beklenti oluştur")
    @PostMapping
    public ResponseEntity<ApiResponse<ExpectationResponse>> create(
            @RequestBody CreateExpectationRequest request,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(expectationService.createExpectation(user, request)));
    }

    @Operation(summary = "Aktif beklentileri listele")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExpectationResponse>>> list(Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(expectationService.getExpectations(user)));
    }

    @Operation(summary = "Beklenti detayını getir")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpectationResponse>> get(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(expectationService.getExpectation(user, id)));
    }

    @Operation(summary = "Beklentiyi güncelle")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpectationResponse>> update(
            @PathVariable UUID id,
            @RequestBody CreateExpectationRequest request,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(expectationService.updateExpectation(user, id, request)));
    }

    @Operation(summary = "Beklentiyi deaktif et (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        expectationService.deleteExpectation(user, id);
        return ResponseEntity.ok(ApiResponse.success("Beklenti deaktif edildi.", null));
    }

    @Operation(summary = "Pasif beklentiyi yeniden aktifleştir")
    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ExpectationResponse>> activate(
            @PathVariable UUID id,
            Authentication auth) {
        User user = securityHelper.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success(expectationService.activateExpectation(user, id)));
    }
}
