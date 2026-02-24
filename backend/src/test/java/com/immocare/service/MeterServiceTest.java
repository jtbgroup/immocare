package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.exception.MeterBusinessRuleException;
import com.immocare.exception.MeterNotFoundException;
import com.immocare.mapper.MeterMapper;
import com.immocare.model.dto.AddMeterRequest;
import com.immocare.model.dto.MeterDTO;
import com.immocare.model.dto.ReplaceMeterRequest;
import com.immocare.model.entity.Meter;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.MeterRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("MeterService")
class MeterServiceTest {

    @Mock
    MeterRepository meterRepository;
    @Mock
    HousingUnitRepository housingUnitRepository;
    @Mock
    BuildingRepository buildingRepository;
    @Mock
    MeterMapper meterMapper;

    @InjectMocks
    MeterService meterService;

    private static final Long UNIT_ID = 1L;
    private static final Long BUILDING_ID = 2L;
    private static final Long METER_ID = 10L;
    private static final String HU = "HOUSING_UNIT";
    private static final String BLD = "BUILDING";

    @BeforeEach
    void stubOwnerExists() {
        when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
        when(buildingRepository.existsById(BUILDING_ID)).thenReturn(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADD METER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addMeter")
    class AddMeter {

        @Test
        @DisplayName("TS-UC008-01 — GAS without eanCode → MeterBusinessRuleException")
        void addGasMeter_withoutEanCode_throwsException() {
            var req = new AddMeterRequest("GAS", "GAS-001", "GAS meter", null, null, null, LocalDate.now());
            assertThatThrownBy(() -> meterService.addMeter(HU, UNIT_ID, req))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("EAN code is required for gas meters");
        }

        @Test
        @DisplayName("TS-UC008-01b — ELECTRICITY without eanCode → MeterBusinessRuleException")
        void addElectricityMeter_withoutEanCode_throwsException() {
            var req = new AddMeterRequest("ELECTRICITY", "ELC-001", "ELECTRICITY meter", null, null, null,
                    LocalDate.now());
            assertThatThrownBy(() -> meterService.addMeter(HU, UNIT_ID, req))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("EAN code is required for electricity meters");
        }

        @Test
        @DisplayName("TS-UC008-02 — WATER on BUILDING without customerNumber → MeterBusinessRuleException")
        void addWaterMeter_buildingWithoutCustomerNumber_throwsException() {
            var req = new AddMeterRequest("WATER", "WTR-B01", "WATER meter", null, "IN-001", null, LocalDate.now());
            assertThatThrownBy(() -> meterService.addMeter(BLD, BUILDING_ID, req))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("Customer number is required for water meters on a building");
        }

        @Test
        @DisplayName("TS-UC008-02b — WATER on HOUSING_UNIT without customerNumber → OK (not required)")
        void addWaterMeter_housingUnitWithoutCustomerNumber_succeeds() {
            var req = new AddMeterRequest("WATER", "WTR-001", "WATER meter", null, "IN-001", null, LocalDate.now());
            Meter saved = buildMeter("WATER", HU, UNIT_ID, LocalDate.now(), null);
            when(meterRepository.save(any())).thenReturn(saved);
            MeterDTO dto = buildDTO(saved);
            when(meterMapper.toDTO(saved)).thenReturn(dto);

            MeterDTO result = meterService.addMeter(HU, UNIT_ID, req);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("TS-UC008-03 — startDate in future → MeterBusinessRuleException")
        void addMeter_futurStartDate_throwsException() {
            var req = new AddMeterRequest("GAS", "GAS-001", "GAS meter", "54100000000001",
                    null, null, LocalDate.now().plusDays(1));
            assertThatThrownBy(() -> meterService.addMeter(HU, UNIT_ID, req))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("Start date cannot be in the future");
        }

        @Test
        @DisplayName("TS-UC008-06 — Two ELECTRICITY meters on same unit → both saved")
        void addTwoElectricityMeters_sameUnit_bothSaved() {
            var req1 = new AddMeterRequest("ELECTRICITY", "ELC-001", "ELECTRICITY1 meter", "54200000000001",
                    null, null, LocalDate.now());
            var req2 = new AddMeterRequest("ELECTRICITY", "ELC-002", "ELECTRICITY2 meter", "54200000000002",
                    null, null, LocalDate.now());

            Meter m1 = buildMeter("ELECTRICITY", HU, UNIT_ID, LocalDate.now(), null);
            Meter m2 = buildMeter("ELECTRICITY", HU, UNIT_ID, LocalDate.now(), null);
            when(meterRepository.save(any())).thenReturn(m1).thenReturn(m2);
            when(meterMapper.toDTO(any())).thenReturn(buildDTO(m1)).thenReturn(buildDTO(m2));

            MeterDTO r1 = meterService.addMeter(HU, UNIT_ID, req1);
            MeterDTO r2 = meterService.addMeter(HU, UNIT_ID, req2);

            assertThat(r1).isNotNull();
            assertThat(r2).isNotNull();
            verify(meterRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Valid GAS meter → saved with endDate = null")
        void addGasMeter_valid_savedActive() {
            var req = new AddMeterRequest("GAS", "GAS-001", "GAS meter", "54100000000001",
                    null, null, LocalDate.now());
            Meter saved = buildMeter("GAS", HU, UNIT_ID, LocalDate.now(), null);
            when(meterRepository.save(any())).thenReturn(saved);
            when(meterMapper.toDTO(saved)).thenReturn(buildDTO(saved));

            MeterDTO result = meterService.addMeter(HU, UNIT_ID, req);

            assertThat(result).isNotNull();
            ArgumentCaptor<Meter> captor = ArgumentCaptor.forClass(Meter.class);
            verify(meterRepository).save(captor.capture());
            assertThat(captor.getValue().getEndDate()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REPLACE METER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("replaceMeter")
    class ReplaceMeter {

        @Test
        @DisplayName("TS-UC008-04 — newStartDate before current startDate → MeterBusinessRuleException")
        void replaceMeter_newStartDateBeforeCurrent_throwsException() {
            LocalDate currentStart = LocalDate.of(2024, 6, 1);
            Meter current = buildMeter("ELECTRICITY", HU, UNIT_ID, currentStart, null);
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.of(current));

            var req = new ReplaceMeterRequest("ELC-002", "54200000000002", "ELECTRICITY meter",
                    null, null, LocalDate.of(2024, 5, 1), null);

            assertThatThrownBy(() -> meterService.replaceMeter(HU, UNIT_ID, METER_ID, req))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("Start date must be ≥ current meter start date");
        }

        @Test
        @DisplayName("Meter not found → MeterNotFoundException")
        void replaceMeter_notFound_throwsException() {
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.empty());
            var req = new ReplaceMeterRequest("ELC-002", "54200000000002", "ELECTRICITY meter",
                    null, null, LocalDate.now(), null);

            assertThatThrownBy(() -> meterService.replaceMeter(HU, UNIT_ID, METER_ID, req))
                    .isInstanceOf(MeterNotFoundException.class);
        }

        @Test
        @DisplayName("TS-UC008-07 — valid replace → old closed, new active")
        void replaceMeter_valid_oldClosedNewActive() {
            LocalDate currentStart = LocalDate.of(2023, 1, 1);
            LocalDate newStart = LocalDate.of(2024, 1, 1);
            Meter current = buildMeter("ELECTRICITY", HU, UNIT_ID, currentStart, null);
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.of(current));

            Meter newMeter = buildMeter("ELECTRICITY", HU, UNIT_ID, newStart, null);
            when(meterRepository.save(any())).thenReturn(current).thenReturn(newMeter);
            when(meterMapper.toDTO(newMeter)).thenReturn(buildDTO(newMeter));

            var req = new ReplaceMeterRequest("ELC-002", "54200000000002", "ELECTRICITY meter",
                    null, null, newStart, "UPGRADE");

            MeterDTO result = meterService.replaceMeter(HU, UNIT_ID, METER_ID, req);

            assertThat(result).isNotNull();
            // Verify old meter was closed
            assertThat(current.getEndDate()).isEqualTo(newStart);
            // Two saves: one for old (close), one for new
            verify(meterRepository, times(2)).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REMOVE METER
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeMeter")
    class RemoveMeter {

        @Test
        @DisplayName("TS-UC008-05 — endDate before startDate → MeterBusinessRuleException")
        void removeMeter_endDateBeforeStartDate_throwsException() {
            LocalDate startDate = LocalDate.of(2024, 6, 1);
            Meter meter = buildMeter("WATER", HU, UNIT_ID, startDate, null);
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.of(meter));

            LocalDate badEndDate = LocalDate.of(2024, 5, 1);
            assertThatThrownBy(() -> meterService.removeMeter(HU, UNIT_ID, METER_ID, badEndDate))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("End date must be ≥ start date");
        }

        @Test
        @DisplayName("endDate in future → MeterBusinessRuleException")
        void removeMeter_futurEndDate_throwsException() {
            Meter meter = buildMeter("WATER", HU, UNIT_ID, LocalDate.now().minusDays(30), null);
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.of(meter));

            assertThatThrownBy(() -> meterService.removeMeter(HU, UNIT_ID, METER_ID,
                    LocalDate.now().plusDays(1)))
                    .isInstanceOf(MeterBusinessRuleException.class)
                    .hasMessageContaining("End date cannot be in the future");
        }

        @Test
        @DisplayName("Valid remove → endDate set on meter")
        void removeMeter_valid_endDateSet() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.now();
            Meter meter = buildMeter("WATER", HU, UNIT_ID, startDate, null);
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.of(meter));
            when(meterRepository.save(any())).thenReturn(meter);

            meterService.removeMeter(HU, UNIT_ID, METER_ID, endDate);

            assertThat(meter.getEndDate()).isEqualTo(endDate);
            verify(meterRepository).save(meter);
        }

        @Test
        @DisplayName("Meter not found → MeterNotFoundException")
        void removeMeter_notFound_throwsException() {
            when(meterRepository.findByIdAndEndDateIsNull(METER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> meterService.removeMeter(HU, UNIT_ID, METER_ID, LocalDate.now()))
                    .isInstanceOf(MeterNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMeterHistory")
    class GetHistory {

        @Test
        @DisplayName("TS-UC008-08 — returns all records sorted by startDate DESC")
        void getMeterHistory_returnsSortedList() {
            Meter m1 = buildMeter("WATER", HU, UNIT_ID, LocalDate.of(2022, 1, 1), LocalDate.of(2023, 1, 1));
            Meter m2 = buildMeter("WATER", HU, UNIT_ID, LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 1));
            Meter m3 = buildMeter("WATER", HU, UNIT_ID, LocalDate.of(2024, 1, 1), null);

            when(meterRepository.findByOwnerTypeAndOwnerIdOrderByStartDateDesc(HU, UNIT_ID))
                    .thenReturn(List.of(m3, m2, m1)); // already sorted by repository

            when(meterMapper.toDTO(any())).thenAnswer(inv -> buildDTO(inv.getArgument(0)));

            List<MeterDTO> result = meterService.getMeterHistory(HU, UNIT_ID);

            assertThat(result).hasSize(3);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private Meter buildMeter(String type, String ownerType, Long ownerId,
            LocalDate startDate, LocalDate endDate) {
        Meter m = new Meter();
        m.setType(type);
        m.setOwnerType(ownerType);
        m.setOwnerId(ownerId);
        m.setMeterNumber("TEST-" + System.nanoTime());
        m.setStartDate(startDate);
        m.setEndDate(endDate);
        return m;
    }

    private MeterDTO buildDTO(Meter m) {
        return new MeterDTO(
                1L, m.getType(), m.getMeterNumber(), m.getLabel(),
                m.getEanCode(), m.getInstallationNumber(), m.getCustomerNumber(),
                m.getOwnerType(), m.getOwnerId(),
                m.getStartDate(), m.getEndDate(),
                m.getEndDate() == null ? "ACTIVE" : "CLOSED",
                null);
    }
}
