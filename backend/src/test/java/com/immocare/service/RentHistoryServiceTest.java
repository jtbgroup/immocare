package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.mapper.RentHistoryMapper;
import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.dto.SetRentRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.RentHistory;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.RentHistoryRepository;
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

@ExtendWith(MockitoExtension.class)
class RentHistoryServiceTest {

    @Mock private RentHistoryRepository rentHistoryRepository;
    @Mock private HousingUnitRepository housingUnitRepository;
    @Mock private RentHistoryMapper rentHistoryMapper;

    @InjectMocks private RentHistoryService rentHistoryService;

    private HousingUnit unit;
    private static final Long UNIT_ID = 1L;

    @BeforeEach
    void setUp() {
        unit = new HousingUnit();
        // Use reflection to set id (no public setter on entity)
        try {
            var field = HousingUnit.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(unit, UNIT_ID);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // -------------------------------------------------------------------------
    // setInitialRent (US021)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("US021 — Sets initial rent when no rent exists")
    void setOrUpdateRent_noExistingRent_createsInitialRent() {
        SetRentRequest request = new SetRentRequest(
                new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), "Initial rate");

        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(false);
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));
        when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(850.00, "2024-01-01", null));

        RentHistoryDTO result = rentHistoryService.setOrUpdateRent(UNIT_ID, request);

        assertThat(result).isNotNull();
        assertThat(result.monthlyRent()).isEqualByComparingTo("850.00");
        verify(rentHistoryRepository, times(1)).save(any());
    }

    // -------------------------------------------------------------------------
    // updateRent (US022)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("US022 — Updates rent: closes old record, creates new one")
    void setOrUpdateRent_existingRent_closesOldAndCreatesNew() {
        SetRentRequest request = new SetRentRequest(
                new BigDecimal("900.00"), LocalDate.of(2024, 7, 1), "Annual increase");

        RentHistory current = buildRentHistory(850.00, "2024-01-01", null);
        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(true);
        when(rentHistoryRepository.findByHousingUnitIdAndEffectiveToIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));
        when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(900.00, "2024-07-01", null));

        RentHistoryDTO result = rentHistoryService.setOrUpdateRent(UNIT_ID, request);

        assertThat(result.monthlyRent()).isEqualByComparingTo("900.00");
        // old record should have been closed
        assertThat(current.getEffectiveTo()).isEqualTo(LocalDate.of(2024, 6, 30));
        // save called twice: close old + create new
        verify(rentHistoryRepository, times(2)).save(any());
    }

    @Test
    @DisplayName("US022 — Effective from auto-closes previous: effectiveTo = effectiveFrom - 1 day")
    void updateRent_autoClosesPreviousRecord_effectiveToIsOneDayBefore() {
        LocalDate newFrom = LocalDate.of(2024, 7, 1);
        SetRentRequest request = new SetRentRequest(new BigDecimal("900.00"), newFrom, null);

        RentHistory current = buildRentHistory(850.00, "2024-01-01", null);
        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(true);
        when(rentHistoryRepository.findByHousingUnitIdAndEffectiveToIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));
        when(rentHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(900.00, "2024-07-01", null));

        rentHistoryService.setOrUpdateRent(UNIT_ID, request);

        assertThat(current.getEffectiveTo()).isEqualTo(newFrom.minusDays(1));
    }

    // -------------------------------------------------------------------------
    // Validation — BR-06 & BR-08
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("BR-06 — Rejects effectiveFrom more than 1 year in future")
    void setOrUpdateRent_dateTooFarInFuture_throwsIllegalArgument() {
        LocalDate tooFar = LocalDate.now().plusYears(1).plusDays(1);
        SetRentRequest request = new SetRentRequest(new BigDecimal("850.00"), tooFar, null);

        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(false);
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> rentHistoryService.setOrUpdateRent(UNIT_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than 1 year in the future");
    }

    @Test
    @DisplayName("BR-08 — Rejects effectiveFrom before current rent's effectiveFrom")
    void updateRent_backdateBeforeCurrent_throwsIllegalArgument() {
        SetRentRequest request = new SetRentRequest(
                new BigDecimal("900.00"), LocalDate.of(2024, 1, 1), null);

        RentHistory current = buildRentHistory(850.00, "2024-06-01", null);
        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(true);
        when(rentHistoryRepository.findByHousingUnitIdAndEffectiveToIsNull(UNIT_ID))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> rentHistoryService.setOrUpdateRent(UNIT_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot backdate");
    }

    // -------------------------------------------------------------------------
    // Unit not found
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Throws HousingUnitNotFoundException when unit does not exist (read)")
    void getCurrentRent_unitNotFound_throws() {
        when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(false);

        assertThatThrownBy(() -> rentHistoryService.getCurrentRent(UNIT_ID))
                .isInstanceOf(HousingUnitNotFoundException.class);
    }

    @Test
    @DisplayName("Throws HousingUnitNotFoundException when unit does not exist (write)")
    void setInitialRent_unitNotFound_throws() {
        SetRentRequest request = new SetRentRequest(
                new BigDecimal("850.00"), LocalDate.of(2024, 1, 1), null);

        when(rentHistoryRepository.existsByHousingUnitId(UNIT_ID)).thenReturn(false);
        when(housingUnitRepository.findById(UNIT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rentHistoryService.setOrUpdateRent(UNIT_ID, request))
                .isInstanceOf(HousingUnitNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getRentHistory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("US023 — Returns full history sorted by effectiveFrom DESC")
    void getRentHistory_returnsAllRecords() {
        when(housingUnitRepository.existsById(UNIT_ID)).thenReturn(true);
        when(rentHistoryRepository.findByHousingUnitIdOrderByEffectiveFromDesc(UNIT_ID))
                .thenReturn(List.of(
                        buildRentHistory(900.00, "2024-07-01", null),
                        buildRentHistory(850.00, "2024-01-01", "2024-06-30")
                ));
        when(rentHistoryMapper.toDTO(any())).thenReturn(stubDTO(900.00, "2024-07-01", null));

        List<RentHistoryDTO> history = rentHistoryService.getRentHistory(UNIT_ID);

        assertThat(history).hasSize(2);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RentHistory buildRentHistory(double amount, String from, String to) {
        RentHistory r = new RentHistory(
                unit,
                BigDecimal.valueOf(amount),
                LocalDate.parse(from),
                to != null ? LocalDate.parse(to) : null,
                null
        );
        return r;
    }

    private RentHistoryDTO stubDTO(double amount, String from, String to) {
        return new RentHistoryDTO(
                1L, UNIT_ID,
                BigDecimal.valueOf(amount),
                LocalDate.parse(from),
                to != null ? LocalDate.parse(to) : null,
                null,
                LocalDateTime.now(),
                to == null, 6L
        );
    }
}
