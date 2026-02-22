package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.RentHistoryMapper;
import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.RentHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.RentHistoryRepository;

@ExtendWith(MockitoExtension.class)
class RentHistoryServiceTest {

        @Mock
        private RentHistoryRepository rentHistoryRepository;
        @Mock
        private HousingUnitRepository housingUnitRepository;
        @Mock
        private RentHistoryMapper rentHistoryMapper;

        @InjectMocks
        private RentHistoryService rentHistoryService;

        private HousingUnit unit;
        private static final Long UNIT_ID = 1L;
        private static final Long RENT_ID = 10L;

        @BeforeEach
        void setUp() {
                unit = new HousingUnit();
                try {
                        var field = HousingUnit.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(unit, UNIT_ID);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        }

        // -------------------------------------------------------------------------
        // addRent
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("addRent — first rent for unit, no previous record")
        void addRent_noPreviousRecord_savesNewRecord() {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), "Initial rate");

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of());
                when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(1L, 850.00, "2024-01-01", null));

                RentHistoryDTO result = rentHistoryService.addRent(UNIT_ID, request);

                assertThat(result).isNotNull();
                assertThat(result.monthlyRent()).isEqualByComparingTo("850.00");
                verify(rentHistoryRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("addRent — new most recent record closes previous")
        void addRent_newMostRecent_closesPreviousRecord() {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("900.00"), LocalDate.of(2024, 7, 1), "Increase");

                RentHistory existing = buildRentHistory(10L, 850.00, "2024-01-01", null);

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(existing));
                when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(2L, 900.00, "2024-07-01", null));

                rentHistoryService.addRent(UNIT_ID, request);

                // Previous record must be closed: effectiveTo = new effectiveFrom - 1 day
                assertThat(existing.getEffectiveTo()).isEqualTo(LocalDate.of(2024, 6, 30));
                // save called twice: close old + save new
                verify(rentHistoryRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("addRent — inserted in middle gets correct effectiveTo")
        void addRent_insertedInMiddle_getsEffectiveToFromNext() {
                // Existing: 850 from 2024-01-01 (current), new record inserted at 2024-04-01
                RentHistory existing = buildRentHistory(10L, 850.00, "2024-01-01", null);
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("870.00"), LocalDate.of(2024, 4, 1), "Adjustment");

                // existing is more recent than new record? No: existing is 2024-01-01, new is
                // 2024-04-01
                // so new becomes the most recent → same as above case
                // Test a true middle insertion: existing has a next record
                RentHistory older = buildRentHistory(9L, 800.00, "2023-01-01", "2023-12-31");
                RentHistory current = buildRentHistory(10L, 850.00, "2024-01-01", null);
                // inserting 870 at 2023-06-01 → between older and current
                SetRentRequest middleRequest = new SetRentRequest(
                                new BigDecimal("820.00"), LocalDate.of(2023, 6, 1), "Middle");

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(current, older));
                when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(11L, 820.00, "2023-06-01", "2023-12-31"));

                RentHistoryDTO result = rentHistoryService.addRent(UNIT_ID, middleRequest);

                assertThat(result).isNotNull();
                // older's effectiveTo should be updated to 2023-05-31
                assertThat(older.getEffectiveTo()).isEqualTo(LocalDate.of(2023, 5, 31));
        }

        @Test
        @DisplayName("addRent — rejects date more than 1 year in future")
        void addRent_dateTooFarInFuture_throws() {
                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("850.00"), LocalDate.now().plusYears(1).plusDays(1), null);

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);

                assertThatThrownBy(() -> rentHistoryService.addRent(UNIT_ID, request))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("more than 1 year in the future");
        }

        @Test
        @DisplayName("addRent — throws when unit not found")
        void addRent_unitNotFound_throws() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(false);

                assertThatThrownBy(() -> rentHistoryService.addRent(UNIT_ID,
                                new SetRentRequest(new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), null)))
                                .isInstanceOf(HousingUnitNotFoundException.class);
        }

        // -------------------------------------------------------------------------
        // updateRent
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("updateRent — updates fields and recalculates neighbours")
        void updateRent_updatesRecordAndRecalculatesNeighbours() {
                RentHistory record = buildRentHistory(RENT_ID, 850.00, "2024-01-01", null);
                record.getHousingUnit(); // ensure lazy-proxy not needed
                // Make housing unit reachable on the record
                setHousingUnit(record, unit);

                SetRentRequest request = new SetRentRequest(
                                new BigDecimal("900.00"), LocalDate.of(2024, 3, 1), "Correction");

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findById(RENT_ID)).thenReturn(Optional.of(record));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(record));
                when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(RENT_ID, 900.00, "2024-03-01", null));

                RentHistoryDTO result = rentHistoryService.updateRent(UNIT_ID, RENT_ID, request);

                assertThat(result).isNotNull();
                assertThat(record.getMonthlyRent()).isEqualByComparingTo("900.00");
                assertThat(record.getEffectiveFrom()).isEqualTo(LocalDate.of(2024, 3, 1));
        }

        @Test
        @DisplayName("updateRent — throws when record not found")
        void updateRent_recordNotFound_throws() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findById(RENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> rentHistoryService.updateRent(UNIT_ID, RENT_ID,
                                new SetRentRequest(new BigDecimal("900.00"), LocalDate.of(2024, 1, 1), null)))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        // -------------------------------------------------------------------------
        // deleteRent
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("deleteRent — deletes record and restores previous effectiveTo")
        void deleteRent_currentRecord_restoresPrevious() {
                RentHistory older = buildRentHistory(9L, 800.00, "2023-01-01", "2023-12-31");
                RentHistory current = buildRentHistory(RENT_ID, 850.00, "2024-01-01", null);
                setHousingUnit(current, unit);

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findById(RENT_ID)).thenReturn(Optional.of(current));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(current, older));

                rentHistoryService.deleteRent(UNIT_ID, RENT_ID);

                // older should now have effectiveTo = null (becomes current)
                assertThat(older.getEffectiveTo()).isNull();
                verify(rentHistoryRepository).delete(current);
        }

        @Test
        @DisplayName("deleteRent — middle record: previous inherits deleted effectiveTo")
        void deleteRent_middleRecord_previousInheritsEffectiveTo() {
                RentHistory older = buildRentHistory(8L, 800.00, "2023-01-01", "2023-05-31");
                RentHistory middle = buildRentHistory(9L, 820.00, "2023-06-01", "2023-12-31");
                RentHistory current = buildRentHistory(10L, 850.00, "2024-01-01", null);
                setHousingUnit(middle, unit);

                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findById(9L)).thenReturn(Optional.of(middle));
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(current, middle, older));

                rentHistoryService.deleteRent(UNIT_ID, 9L);

                // older should inherit middle's effectiveTo = 2023-12-31
                assertThat(older.getEffectiveTo()).isEqualTo(LocalDate.of(2023, 12, 31));
                verify(rentHistoryRepository).delete(middle);
        }

        @Test
        @DisplayName("deleteRent — throws when record not found")
        void deleteRent_recordNotFound_throws() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findById(RENT_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> rentHistoryService.deleteRent(UNIT_ID, RENT_ID))
                                .isInstanceOf(IllegalArgumentException.class);
        }

        // -------------------------------------------------------------------------
        // getCurrentRent / getRentHistory
        // -------------------------------------------------------------------------

        @Test
        @DisplayName("getCurrentRent — returns present optional")
        void getCurrentRent_returnsCurrentRecord() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findByHousingUnitIdAndEffectiveToIsNull(UNIT_ID))
                                .thenReturn(Optional.of(buildRentHistory(RENT_ID, 850.00, "2024-01-01", null)));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(RENT_ID, 850.00, "2024-01-01", null));

                Optional<RentHistoryDTO> result = rentHistoryService.getCurrentRent(UNIT_ID);

                assertThat(result).isPresent();
                assertThat(result.get().monthlyRent()).isEqualByComparingTo("850.00");
        }

        @Test
        @DisplayName("getCurrentRent — throws when unit not found")
        void getCurrentRent_unitNotFound_throws() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(false);

                assertThatThrownBy(() -> rentHistoryService.getCurrentRent(UNIT_ID))
                                .isInstanceOf(HousingUnitNotFoundException.class);
        }

        @Test
        @DisplayName("getRentHistory — returns full list")
        void getRentHistory_returnsAllRecords() {
                when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
                when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                                .thenReturn(List.of(
                                                buildRentHistory(2L, 900.00, "2024-07-01", null),
                                                buildRentHistory(1L, 850.00, "2024-01-01", "2024-06-30")));
                when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(2L, 900.00, "2024-07-01", null));

                List<RentHistoryDTO> history = rentHistoryService.getRentHistory(UNIT_ID);

                assertThat(history).hasSize(2);
        }

        // -------------------------------------------------------------------------
        // Helpers
        // -------------------------------------------------------------------------

        private RentHistory buildRentHistory(Long id, double amount, String from, String to) {
                RentHistory r = new RentHistory(
                                unit,
                                BigDecimal.valueOf(amount),
                                LocalDate.parse(from),
                                to != null ? LocalDate.parse(to) : null,
                                null);
                try {
                        var field = RentHistory.class.getDeclaredField("id");
                        field.setAccessible(true);
                        field.set(r, id);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                return r;
        }

        private void setHousingUnit(RentHistory record, HousingUnit hu) {
                try {
                        var field = RentHistory.class.getDeclaredField("housingUnit");
                        field.setAccessible(true);
                        field.set(record, hu);
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
        }

        private RentHistoryDTO stubDTO(Long id, double amount, String from, String to) {
                return new RentHistoryDTO(
                                id, UNIT_ID,
                                BigDecimal.valueOf(amount),
                                LocalDate.parse(from),
                                to != null ? LocalDate.parse(to) : null,
                                null,
                                LocalDateTime.now(),
                                to == null,
                                6L);
        }
}