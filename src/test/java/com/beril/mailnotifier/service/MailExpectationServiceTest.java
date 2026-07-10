package com.beril.mailnotifier.service;

import com.beril.mailnotifier.domain.entity.MailExpectation;
import com.beril.mailnotifier.domain.entity.User;
import com.beril.mailnotifier.domain.repository.MailExpectationRepository;
import com.beril.mailnotifier.domain.repository.MailMatchRepository;
import com.beril.mailnotifier.dto.CreateExpectationRequest;
import com.beril.mailnotifier.dto.ExpectationResponse;
import com.beril.mailnotifier.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailExpectationServiceTest {

    @Mock
    private MailExpectationRepository expectationRepository;

    @Mock
    private MailMatchRepository matchRepository;

    @InjectMocks
    private MailExpectationService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .name("Test Kullanıcı")
                .build();
    }

    // ── createExpectation ─────────────────────────────────────────────────

    @Test
    void createExpectation_sender_ile_basarili_olusturulmali() {
        CreateExpectationRequest request = new CreateExpectationRequest("ahmet@firma.com", null, "Test beklentisi");

        MailExpectation saved = MailExpectation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .senderIdentifier("ahmet@firma.com")
                .description("Test beklentisi")
                .isActive(true)
                .build();

        when(expectationRepository.save(any())).thenReturn(saved);
        when(matchRepository.countByExpectation_IdAndDismissedFalse(saved.getId())).thenReturn(0L);

        ExpectationResponse response = service.createExpectation(testUser, request);

        assertThat(response.senderIdentifier()).isEqualTo("ahmet@firma.com");
        assertThat(response.isActive()).isTrue();
        verify(expectationRepository).save(any(MailExpectation.class));
    }

    @Test
    void createExpectation_keywords_ile_basarili_olusturulmali() {
        CreateExpectationRequest request = new CreateExpectationRequest(null, List.of("teklif", "fatura"), null);

        MailExpectation saved = MailExpectation.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .keywords(List.of("teklif", "fatura"))
                .isActive(true)
                .build();

        when(expectationRepository.save(any())).thenReturn(saved);
        when(matchRepository.countByExpectation_IdAndDismissedFalse(saved.getId())).thenReturn(0L);

        ExpectationResponse response = service.createExpectation(testUser, request);

        assertThat(response.keywords()).containsExactly("teklif", "fatura");
    }

    @Test
    void createExpectation_sender_ve_keywords_bos_ise_exception_firlatmali() {
        CreateExpectationRequest request = new CreateExpectationRequest("", List.of(), null);

        assertThatThrownBy(() -> service.createExpectation(testUser, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("En az bir kriter");

        verifyNoInteractions(expectationRepository);
    }

    @Test
    void createExpectation_null_sender_ve_keywords_ise_exception_firlatmali() {
        CreateExpectationRequest request = new CreateExpectationRequest(null, null, "sadece açıklama");

        assertThatThrownBy(() -> service.createExpectation(testUser, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── deleteExpectation (soft delete) ───────────────────────────────────

    @Test
    void deleteExpectation_isActive_false_yapilmali() {
        UUID id = UUID.randomUUID();
        MailExpectation entity = MailExpectation.builder()
                .id(id)
                .user(testUser)
                .senderIdentifier("ornek@firma.com")
                .isActive(true)
                .build();

        when(expectationRepository.findByIdAndUser_Id(id, testUser.getId()))
                .thenReturn(Optional.of(entity));
        when(expectationRepository.save(any())).thenReturn(entity);

        service.deleteExpectation(testUser, id);

        assertThat(entity.getIsActive()).isFalse();
        verify(expectationRepository).save(entity);
    }

    // ── Başka kullanıcının beklentisine erişim engeli ─────────────────────

    @Test
    void getExpectation_baska_kullanici_id_ile_exception_firlatmali() {
        UUID id = UUID.randomUUID();
        when(expectationRepository.findByIdAndUser_Id(id, testUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getExpectation(testUser, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateExpectation_baska_kullanici_id_ile_exception_firlatmali() {
        UUID id = UUID.randomUUID();
        CreateExpectationRequest request = new CreateExpectationRequest("yeni@firma.com", null, null);

        when(expectationRepository.findByIdAndUser_Id(id, testUser.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateExpectation(testUser, id, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── activateExpectation ───────────────────────────────────────────────

    @Test
    void activateExpectation_isActive_true_yapilmali() {
        UUID id = UUID.randomUUID();
        MailExpectation entity = MailExpectation.builder()
                .id(id)
                .user(testUser)
                .senderIdentifier("ornek@firma.com")
                .isActive(false)
                .build();

        when(expectationRepository.findByIdAndUser_Id(id, testUser.getId()))
                .thenReturn(Optional.of(entity));
        when(expectationRepository.save(any())).thenReturn(entity);
        when(matchRepository.countByExpectation_IdAndDismissedFalse(id)).thenReturn(0L);

        service.activateExpectation(testUser, id);

        assertThat(entity.getIsActive()).isTrue();
    }
}
