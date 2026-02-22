package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.InvalidPebScoreDateException;
import com.immocare.exception.InvalidValidityPeriodException;
import com.immocare.mapper.PebScoreMapper;
import com.immocare.model.dto.CreatePebScoreRequest;
import com.immocare.model.dto.PebImprovementDTO;
import com.immocare.model.dto.PebScoreDTO;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.PebScore;
import com.immocare.model.entity.PebScoreHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.PebScoreRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PebScoreServiceTest {

    @Mock private PebScoreRepository pebScoreRepository;
    @Mock private HousingUnitRepository housingUnitRepository;
    @Mock private PebScoreMapper pebScoreMapper;

    @InjectMocks private PebScoreService service;

    private HousingUnit unit;

    @BeforeEach
    void setUp() {
        unit = new HousingUnit();
        // set id via reflection-free approach: use a real entity or a spy
    }

    // ─── addScore ─────────────────────────────────────────────────────────────

    @Test
    void addScore_unitNotFound_throwsHousingUnitNotFoundException() {
        when(housingUnitRepository.findById(99L)).thenReturn(Optional.empty());

        CreatePebScoreRequest req = validRequest();
        assertThatThrownBy(() -> service.addScore(99L, req))
                .isInstanceOf(HousingUnitNotFoundException.class);

        verify(pebScoreRepository, never()).save(any());
    }

    @Test
    void addScore_futureDate_throwsInvalidPebScoreDateException() {
        when(housingUnitRepository.findById(1L)).thenReturn(Optional.of(unit));

        CreatePebScoreRequest req = validRequest();
        req.setScoreDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> service.addScore(1L, req))
                .isInstanceOf(InvalidPebScoreDateException.class)
                .hasMessageContaining("future");
    }

    @Test
    void addScore_validUntilBeforeScoreDate_throwsInvalidValidityPeriodException() {
        when(housingUnitRepository.findById(1L)).thenReturn(Optional.of(unit));

        CreatePebScoreRequest req = validRequest();
        req.setValidUntil(req.getScoreDate().minusDays(1));

        assertThatThrownBy(() -> service.addScore(1L, req))
                .isInstanceOf(InvalidValidityPeriodException.class);
    }

    @Test
    void addScore_valid_savesAndReturnsDTO() {
        when(housingUnitRepository.findById(1L)).thenReturn(Optional.of(unit));
        PebScoreHistory saved = buildEntity(PebScore.B, LocalDate.now(), null);
        when(pebScoreRepository.save(any())).thenReturn(saved);
        PebScoreDTO dto = new PebScoreDTO();
        dto.setPebScore(PebScore.B);
        when(pebScoreMapper.toDTO(saved)).thenReturn(dto);

        PebScoreDTO result = service.addScore(1L, validRequest());

        assertThat(result.getPebScore()).isEqualTo(PebScore.B);
        assertThat(result.getStatus()).isEqualTo("CURRENT");
        verify(pebScoreRepository).save(any());
    }

    // ─── Status computation ───────────────────────────────────────────────────

    @Test
    void getHistory_computesExpiredStatus_whenValidUntilPast() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        PebScoreHistory expired = buildEntity(PebScore.C, LocalDate.now().minusYears(2),
                LocalDate.now().minusDays(1));
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L))
                .thenReturn(List.of(expired));
        PebScoreDTO dto = new PebScoreDTO();
        dto.setValidUntil(expired.getValidUntil());
        when(pebScoreMapper.toDTO(expired)).thenReturn(dto);

        List<PebScoreDTO> result = service.getHistory(1L);

        assertThat(result.get(0).getStatus()).isEqualTo("EXPIRED");
        assertThat(result.get(0).getExpiryWarning()).isEqualTo("EXPIRED");
    }

    @Test
    void getHistory_computesExpiringSoon_whenValidUntilWithin3Months() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        LocalDate soonDate = LocalDate.now().plusMonths(1);
        PebScoreHistory rec = buildEntity(PebScore.B, LocalDate.now().minusYears(1), soonDate);
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L)).thenReturn(List.of(rec));
        PebScoreDTO dto = new PebScoreDTO();
        dto.setValidUntil(soonDate);
        when(pebScoreMapper.toDTO(rec)).thenReturn(dto);

        List<PebScoreDTO> result = service.getHistory(1L);

        assertThat(result.get(0).getExpiryWarning()).isEqualTo("EXPIRING_SOON");
    }

    @Test
    void getHistory_noDate_returnsNoDateWarning() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        PebScoreHistory rec = buildEntity(PebScore.A, LocalDate.now().minusYears(1), null);
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L)).thenReturn(List.of(rec));
        PebScoreDTO dto = new PebScoreDTO();
        when(pebScoreMapper.toDTO(rec)).thenReturn(dto);

        List<PebScoreDTO> result = service.getHistory(1L);

        assertThat(result.get(0).getExpiryWarning()).isEqualTo("NO_DATE");
    }

    // ─── getImprovementSummary ─────────────────────────────────────────────────

    @Test
    void getImprovementSummary_computesImprovedGrades() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        // D (2020) → B (2024): improvement of 2 grades
        PebScoreHistory rec2024 = buildEntity(PebScore.B, LocalDate.of(2024, 1, 1), null);
        PebScoreHistory rec2020 = buildEntity(PebScore.D, LocalDate.of(2020, 1, 1), null);
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L))
                .thenReturn(List.of(rec2024, rec2020));

        Optional<PebImprovementDTO> result = service.getImprovementSummary(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getGradesImproved()).isEqualTo(2);
        assertThat(result.get().getFirstScore()).isEqualTo(PebScore.D);
        assertThat(result.get().getCurrentScore()).isEqualTo(PebScore.B);
        assertThat(result.get().getHistory()).hasSize(1);
        assertThat(result.get().getHistory().get(0).getDirection()).isEqualTo("IMPROVED");
    }

    @Test
    void getImprovementSummary_degraded_returnsNegativeGrades() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        PebScoreHistory rec2024 = buildEntity(PebScore.D, LocalDate.of(2024, 1, 1), null);
        PebScoreHistory rec2020 = buildEntity(PebScore.B, LocalDate.of(2020, 1, 1), null);
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L))
                .thenReturn(List.of(rec2024, rec2020));

        Optional<PebImprovementDTO> result = service.getImprovementSummary(1L);

        assertThat(result.get().getGradesImproved()).isNegative();
        assertThat(result.get().getHistory().get(0).getDirection()).isEqualTo("DEGRADED");
    }

    @Test
    void getImprovementSummary_noHistory_returnsEmpty() {
        when(housingUnitRepository.existsById(1L)).thenReturn(true);
        when(pebScoreRepository.findByHousingUnitIdOrderByScoreDateDesc(1L)).thenReturn(List.of());

        Optional<PebImprovementDTO> result = service.getImprovementSummary(1L);

        assertThat(result).isEmpty();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CreatePebScoreRequest validRequest() {
        CreatePebScoreRequest req = new CreatePebScoreRequest();
        req.setPebScore(PebScore.B);
        req.setScoreDate(LocalDate.now());
        return req;
    }

    private PebScoreHistory buildEntity(PebScore score, LocalDate scoreDate, LocalDate validUntil) {
        PebScoreHistory h = new PebScoreHistory();
        h.setPebScore(score);
        h.setScoreDate(scoreDate);
        h.setValidUntil(validUntil);
        h.setHousingUnit(unit);
        return h;
    }
}
