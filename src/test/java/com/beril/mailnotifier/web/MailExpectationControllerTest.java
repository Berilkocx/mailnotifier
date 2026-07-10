package com.beril.mailnotifier.web;

import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.dto.CreateExpectationRequest;
import com.beril.mailnotifier.dto.ExpectationResponse;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import com.beril.mailnotifier.security.SecurityHelper;
import com.beril.mailnotifier.service.MailExpectationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MailExpectationController.class)
class MailExpectationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MailExpectationService expectationService;

    @MockitoBean
    private SecurityHelper securityHelper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test Kullanıcı")
                .build();
        when(securityHelper.getCurrentUser(any())).thenReturn(testUser);
    }

    // ── GET /api/expectations ─────────────────────────────────────────────

    @Test
    @WithMockUser
    void list_200_ve_bos_liste_donmeli() throws Exception {
        when(expectationService.getExpectations(testUser)).thenReturn(List.of());

        mockMvc.perform(get("/api/expectations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser
    void list_beklentiler_ile_200_donmeli() throws Exception {
        ExpectationResponse resp = new ExpectationResponse(
                UUID.randomUUID(), "ahmet@firma.com", List.of("teklif"),
                "Test", true, 3L, LocalDateTime.now(), null
        );
        when(expectationService.getExpectations(testUser)).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/expectations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].senderIdentifier").value("ahmet@firma.com"))
                .andExpect(jsonPath("$.data[0].matchCount").value(3));
    }

    // ── POST /api/expectations ────────────────────────────────────────────

    @Test
    @WithMockUser
    void create_gecerli_body_ile_201_donmeli() throws Exception {
        CreateExpectationRequest req = new CreateExpectationRequest("ahmet@firma.com", List.of("teklif"), null);
        ExpectationResponse resp = new ExpectationResponse(
                UUID.randomUUID(), "ahmet@firma.com", List.of("teklif"),
                null, true, 0L, LocalDateTime.now(), null
        );

        when(expectationService.createExpectation(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/expectations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.senderIdentifier").value("ahmet@firma.com"));
    }

    @Test
    @WithMockUser
    void create_service_exception_firlatirsa_400_donmeli() throws Exception {
        CreateExpectationRequest req = new CreateExpectationRequest("", List.of(), null);

        when(expectationService.createExpectation(any(), any()))
                .thenThrow(new IllegalArgumentException("En az bir kriter girilmeli"));

        mockMvc.perform(post("/api/expectations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/expectations/{id} ────────────────────────────────────────

    @Test
    @WithMockUser
    void get_mevcut_id_ile_200_donmeli() throws Exception {
        UUID id = UUID.randomUUID();
        ExpectationResponse resp = new ExpectationResponse(
                id, "firma.com", List.of(), "Açıklama", true, 1L, LocalDateTime.now(), null
        );

        when(expectationService.getExpectation(testUser, id)).thenReturn(resp);

        mockMvc.perform(get("/api/expectations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @WithMockUser
    void get_baska_kullanici_id_ile_404_donmeli() throws Exception {
        UUID id = UUID.randomUUID();

        when(expectationService.getExpectation(testUser, id))
                .thenThrow(new ResourceNotFoundException("Beklenti", "id", id));

        mockMvc.perform(get("/api/expectations/{id}", id))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /api/expectations/{id} ─────────────────────────────────────

    @Test
    @WithMockUser
    void delete_mevcut_id_ile_200_donmeli() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/expectations/{id}", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ── POST /api/expectations/{id}/activate ──────────────────────────────

    @Test
    @WithMockUser
    void activate_pasif_beklenti_200_donmeli() throws Exception {
        UUID id = UUID.randomUUID();
        ExpectationResponse resp = new ExpectationResponse(
                id, "firma.com", List.of(), null, true, 0L, LocalDateTime.now(), null
        );

        when(expectationService.activateExpectation(testUser, id)).thenReturn(resp);

        mockMvc.perform(post("/api/expectations/{id}/activate", id)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }
}
