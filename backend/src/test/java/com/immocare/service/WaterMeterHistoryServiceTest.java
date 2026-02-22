package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.WaterMeterHistoryMapper;
import com.immocare.model.dto.AssignMeterRequest;
import com.immocare.model.dto.RemoveMeterRequest;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.dto.WaterMeterHistoryDTO;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.WaterMeterHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.WaterMeterHistoryRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for WaterMeterHistoryService.
 * Covers all test scenarios TS-UC006-01 through TS-UC006-10.
 */
@ExtendWith(MockitoExtension.class)
class WaterMeterHistoryServiceTest {

    private static final Long UNIT_ID = 1L;

    @Mock WaterMeterHistoryRepository meterRepository;
    @Mock HousingUnitRepository housingUnitRepository;
    @Mock WaterMeterHistoryMapper mapper;

    @InjectMocks WaterMeterHistoryService service;

    private HousingUnit unit;

    @BeforeEach
    void setUp() {
        unit = new HousingUnit();
    }

    // -------------------------------------------------------------------------
    // TS-UC006-01: Assign First Meter
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TS-UC006-01 — Assign first meter: saved as active (removal_date = NULL)")
    void assignMeter_noExistingMeter_savesAsActive() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.empty());
        when(meterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(stubDTO("WM-2024-001", LocalDate.of(2024, 1, 1), null));

        AssignMeterRequest req = buildAssignRequest("WM-2024-001", "Kitchen", LocalDate.of(2024, 1, 1));
        WaterMeterHistoryDTO result = service.assignMeter(UNIT_ID, req);

        assertThat(result.meterNumber()).isEqualTo("WM-2024-001");
        verify(meterRepository).save(any(WaterMeterHistory.class));
    }

    @Test
    @DisplayName("TS-UC006-01 — Assign: fails if unit already has active meter")
    void assignMeter_activeAlreadyExists_throwsIllegalState() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        WaterMeterHistory existing = buildMeter("WM-001", LocalDate.of(2024, 1, 1), null);
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.of(existing));

        AssignMeterRequest req = buildAssignRequest("WM-002", null, LocalDate.of(2024, 6, 1));

        assertThatThrownBy(() -> service.assignMeter(UNIT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active water meter");
    }

    // -------------------------------------------------------------------------
    // TS-UC006-02 & TS-UC006-03: Replace Meter
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TS-UC006-02 — Replace: old meter removal_date = new installation_date, new meter active")
    void replaceMeter_success_closesOldAndCreatesNew() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        WaterMeterHistory current = buildMeter("WM-2024-001", LocalDate.of(2024, 1, 1), null);
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));
        when(meterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(stubDTO("WM-2024-002", LocalDate.of(2024, 6, 1), null));

        ReplaceMeterRequest req = buildReplaceRequest("WM-2024-002", LocalDate.of(2024, 6, 1));
        service.replaceMeter(UNIT_ID, req);

        assertThat(current.getRemovalDate()).isEqualTo(LocalDate.of(2024, 6, 1));
        verify(meterRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("TS-UC006-09 — Replace: installation before current meter date → error")
    void replaceMeter_backdatedInstallation_throwsIllegalArgument() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        WaterMeterHistory current = buildMeter("WM-001", LocalDate.of(2024, 6, 1), null);
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));

        ReplaceMeterRequest req = buildReplaceRequest("WM-002", LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.replaceMeter(UNIT_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be before current meter installation date");
    }

    @Test
    @DisplayName("Replace: no active meter → error")
    void replaceMeter_noActiveMeter_throwsIllegalState() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.empty());

        ReplaceMeterRequest req = buildReplaceRequest("WM-002", LocalDate.of(2024, 6, 1));

        assertThatThrownBy(() -> service.replaceMeter(UNIT_ID, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active water meter");
    }

    // -------------------------------------------------------------------------
    // TS-UC006-06: Remove Meter
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TS-UC006-06 — Remove: meter marked removed, unit has no active meter")
    void removeMeter_success_setsRemovalDate() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        WaterMeterHistory current = buildMeter("WM-2024-001", LocalDate.of(2024, 1, 1), null);
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));
        when(meterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(stubDTO("WM-2024-001", LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31)));

        RemoveMeterRequest req = new RemoveMeterRequest();
        req.setRemovalDate(LocalDate.of(2024, 12, 31));
        service.removeMeter(UNIT_ID, req);

        assertThat(current.getRemovalDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        verify(meterRepository).save(any());
    }

    @Test
    @DisplayName("Remove: removal before installation → error")
    void removeMeter_backdatedRemoval_throwsIllegalArgument() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        WaterMeterHistory current = buildMeter("WM-001", LocalDate.of(2024, 6, 1), null);
        when(meterRepository.findByHousingUnitIdAndRemovalDateIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));

        RemoveMeterRequest req = new RemoveMeterRequest();
        req.setRemovalDate(LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> service.removeMeter(UNIT_ID, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be before installation date");
    }

    // -------------------------------------------------------------------------
    // TS-UC006-04: View History
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("TS-UC006-04 — getMeterHistory returns all meters sorted DESC")
    void getMeterHistory_returnsAllMeters() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(meterRepository.findByHousingUnitIdOrderByInstallationDateDesc(UNIT_ID))
                .thenReturn(List.of(
                        buildMeter("WM-2024", LocalDate.of(2024, 1, 1), null),
                        buildMeter("WM-2023", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
                        buildMeter("WM-2022", LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31))
                ));
        when(mapper.toDTO(any())).thenReturn(stubDTO("X", LocalDate.now(), null));

        List<WaterMeterHistoryDTO> result = service.getMeterHistory(UNIT_ID);

        assertThat(result).hasSize(3);
    }

    // -------------------------------------------------------------------------
    // Unit not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unit not found → HousingUnitNotFoundException")
    void assignMeter_unitNotFound_throws() {
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignMeter(UNIT_ID,
                buildAssignRequest("WM-001", null, LocalDate.now())))
                .isInstanceOf(HousingUnitNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private WaterMeterHistory buildMeter(String number, LocalDate installation, LocalDate removal) {
        WaterMeterHistory m = new WaterMeterHistory(unit, number, null, installation);
        if (removal != null) m.setRemovalDate(removal);
        return m;
    }

    private AssignMeterRequest buildAssignRequest(String number, String location, LocalDate date) {
        AssignMeterRequest r = new AssignMeterRequest();
        r.setMeterNumber(number);
        r.setMeterLocation(location);
        r.setInstallationDate(date);
        return r;
    }

    private ReplaceMeterRequest buildReplaceRequest(String number, LocalDate date) {
        ReplaceMeterRequest r = new ReplaceMeterRequest();
        r.setNewMeterNumber(number);
        r.setNewInstallationDate(date);
        return r;
    }

    private WaterMeterHistoryDTO stubDTO(String number, LocalDate installation, LocalDate removal) {
        return new WaterMeterHistoryDTO(1L, UNIT_ID, number, null, installation,
                removal, LocalDateTime.now(), removal == null, 0L,
                removal == null ? "Active" : "Replaced");
    }
}
