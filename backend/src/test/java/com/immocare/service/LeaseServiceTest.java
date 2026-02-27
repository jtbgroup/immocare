package com.immocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.immocare.exception.LeaseNotEditableException;
import com.immocare.exception.LeaseOverlapException;
import com.immocare.exception.LeaseStatusTransitionException;
import com.immocare.model.dto.AddTenantRequest;
import com.immocare.model.dto.ChangeLeaseStatusRequest;
import com.immocare.model.dto.CreateLeaseRequest;
import com.immocare.model.dto.LeaseDTO;
import com.immocare.model.dto.LeaseSummaryDTO;
import com.immocare.model.dto.RecordIndexationRequest;
import com.immocare.model.dto.UpdateLeaseRequest;
import com.immocare.model.entity.Building;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Lease;
import com.immocare.model.entity.LeaseTenant;
import com.immocare.model.entity.Person;
import com.immocare.model.enums.ChargesType;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;
import com.immocare.model.enums.TenantRole;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseIndexationRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.LeaseTenantRepository;
import com.immocare.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class LeaseServiceTest {

    @Mock
    LeaseRepository leaseRepository;
    @Mock
    LeaseTenantRepository leaseTenantRepository;
    @Mock
    LeaseIndexationRepository indexationRepository;
    @Mock
    HousingUnitRepository housingUnitRepository;
    @Mock
    PersonRepository personRepository;

    @InjectMocks
    LeaseService leaseService;

    private HousingUnit unit;
    private Building building;
    private Person person;
    private Lease lease;

    @BeforeEach
    void setUp() {
        building = new Building();
        building.setId(1L);
        building.setName("Résidence Soleil");
        building.setCity("Brussels");
        building.setCountry("Belgium");

        unit = new HousingUnit();
        unit.setId(10L);
        unit.setUnitNumber("A101");
        unit.setBuilding(building);

        person = new Person();
        person.setId(100L);
        person.setLastName("Dupont");
        person.setFirstName("Jean");

        lease = new Lease();
        lease.setId(1L);
        lease.setHousingUnit(unit);
        lease.setStatus(LeaseStatus.DRAFT);
        lease.setSignatureDate(LocalDate.of(2024, 1, 1));
        lease.setStartDate(LocalDate.of(2024, 2, 1));
        lease.setEndDate(LocalDate.of(2033, 2, 1));
        lease.setLeaseType(LeaseType.MAIN_RESIDENCE_9Y);
        lease.setDurationMonths(108);
        lease.setNoticePeriodMonths(3);
        lease.setMonthlyRent(new BigDecimal("850.00"));
        lease.setMonthlyCharges(new BigDecimal("50.00"));
        lease.setChargesType(ChargesType.FORFAIT);
    }

    // ---- getByUnit ----

    @Test
    @DisplayName("getByUnit returns summary list for given unit")
    void getByUnit_returnsList() {
        when(leaseRepository.findByHousingUnitIdOrderByStartDateDesc(10L)).thenReturn(List.of(lease));
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);
        List<LeaseSummaryDTO> result = leaseService.getByUnit(10L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("DRAFT");
    }

    // ---- create ----

    @Test
    @DisplayName("create lease as DRAFT succeeds with valid data")
    void create_validData_createsDraft() {
        CreateLeaseRequest req = buildCreateRequest();
        when(leaseRepository.existsByHousingUnitIdAndStatusIn(10L, any())).thenReturn(false);
        when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(personRepository.findById(100L)).thenReturn(Optional.of(person));
        when(leaseRepository.save(any())).thenAnswer(inv -> {
            Lease l = inv.getArgument(0);
            l.setId(99L);
            return l;
        });
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);

        LeaseDTO result = leaseService.create(req, false);

        assertThat(result.getStatus()).isEqualTo("DRAFT");
        verify(leaseRepository, atLeastOnce()).save(any());
    }

    @Test
    @DisplayName("create throws LeaseOverlapException when ACTIVE/DRAFT already exists")
    void create_overlap_throws() {
        CreateLeaseRequest req = buildCreateRequest();
        when(leaseRepository.existsByHousingUnitIdAndStatusIn(10L, any())).thenReturn(true);

        assertThatThrownBy(() -> leaseService.create(req, false))
                .isInstanceOf(LeaseOverlapException.class);
    }

    @Test
    @DisplayName("create without PRIMARY tenant throws IllegalArgumentException")
    void create_noPrimaryTenant_throws() {
        CreateLeaseRequest req = buildCreateRequest();
        req.getTenants().get(0).setRole("CO_TENANT"); // no primary
        when(leaseRepository.existsByHousingUnitIdAndStatusIn(10L, any())).thenReturn(false);
        when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));

        assertThatThrownBy(() -> leaseService.create(req, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PRIMARY tenant");
    }

    @Test
    @DisplayName("end_date is correctly calculated from start + duration")
    void create_endDateCalculated() {
        CreateLeaseRequest req = buildCreateRequest();
        when(leaseRepository.existsByHousingUnitIdAndStatusIn(10L, any())).thenReturn(false);
        when(housingUnitRepository.findById(10L)).thenReturn(Optional.of(unit));
        when(personRepository.findById(100L)).thenReturn(Optional.of(person));
        when(leaseRepository.save(any())).thenAnswer(inv -> {
            Lease l = inv.getArgument(0);
            l.setId(1L);
            return l;
        });
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);

        LeaseDTO result = leaseService.create(req, false);
        // start 2024-02-01 + 108 months = 2033-02-01
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2033, 2, 1));
    }

    // ---- changeStatus ----

    @Test
    @DisplayName("DRAFT → ACTIVE succeeds when PRIMARY tenant exists")
    void activate_draftWithPrimary_succeeds() {
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));
        when(leaseRepository.existsByHousingUnitIdAndStatusInAndIdNot(anyLong(), any(), eq(1L))).thenReturn(false);
        when(leaseTenantRepository.countByLeaseIdAndRole(1L, TenantRole.PRIMARY)).thenReturn(1L);
        when(leaseRepository.save(any())).thenReturn(lease);
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);

        ChangeLeaseStatusRequest req = new ChangeLeaseStatusRequest();
        req.setTargetStatus("ACTIVE");

        LeaseDTO result = leaseService.changeStatus(1L, req);
        assertThat(result).isNotNull();
        verify(leaseRepository).save(any());
    }

    @Test
    @DisplayName("ACTIVE → FINISHED works")
    void finish_activeLease_succeeds() {
        lease.setStatus(LeaseStatus.ACTIVE);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any())).thenReturn(lease);
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);

        ChangeLeaseStatusRequest req = new ChangeLeaseStatusRequest();
        req.setTargetStatus("FINISHED");

        leaseService.changeStatus(1L, req);
        verify(leaseRepository).save(argThat(l -> l.getStatus() == LeaseStatus.FINISHED));
    }

    @Test
    @DisplayName("FINISHED → ACTIVE throws LeaseStatusTransitionException")
    void transition_invalidFromFinished_throws() {
        lease.setStatus(LeaseStatus.FINISHED);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));

        ChangeLeaseStatusRequest req = new ChangeLeaseStatusRequest();
        req.setTargetStatus("ACTIVE");

        assertThatThrownBy(() -> leaseService.changeStatus(1L, req))
                .isInstanceOf(LeaseStatusTransitionException.class);
    }

    // ---- recordIndexation ----

    @Test
    @DisplayName("recordIndexation on ACTIVE lease updates rent and creates history")
    void recordIndexation_active_updatesRentAndHistory() {
        lease.setStatus(LeaseStatus.ACTIVE);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));
        when(leaseRepository.save(any())).thenReturn(lease);
        when(indexationRepository.existsByLeaseIdAndYear(any(), anyInt())).thenReturn(false);

        RecordIndexationRequest req = new RecordIndexationRequest();
        req.setApplicationDate(LocalDate.of(2025, 4, 1));
        req.setNewIndexValue(new BigDecimal("125.40"));
        req.setNewIndexMonth(LocalDate.of(2025, 1, 1));
        req.setAppliedRent(new BigDecimal("880.00"));

        verify(indexationRepository).save(any());
        verify(leaseRepository).save(argThat(l -> l.getMonthlyRent().compareTo(new BigDecimal("880.00")) == 0));
    }

    // ---- removeTenant ----

    @Test
    @DisplayName("removing last PRIMARY tenant throws IllegalStateException")
    void removeTenant_lastPrimary_throws() {
        lease.setStatus(LeaseStatus.ACTIVE);
        LeaseTenant lt = new LeaseTenant(lease, person, TenantRole.PRIMARY);
        lease.setTenants(new java.util.ArrayList<>(List.of(lt)));
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));
        when(leaseTenantRepository.countByLeaseIdAndRole(1L, TenantRole.PRIMARY)).thenReturn(1L);

        assertThatThrownBy(() -> leaseService.removeTenant(1L, 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRIMARY");
    }

    // ---- edit FINISHED ----

    @Test
    @DisplayName("updating FINISHED lease throws LeaseNotEditableException")
    void update_finishedLease_throws() {
        lease.setStatus(LeaseStatus.FINISHED);
        when(leaseRepository.findById(1L)).thenReturn(Optional.of(lease));

        assertThatThrownBy(() -> leaseService.update(1L, new UpdateLeaseRequest()))
                .isInstanceOf(LeaseNotEditableException.class);
    }

    // ---- helpers ----

    private CreateLeaseRequest buildCreateRequest() {
        CreateLeaseRequest req = new CreateLeaseRequest();
        req.setHousingUnitId(10L);
        req.setSignatureDate(LocalDate.of(2024, 1, 1));
        req.setStartDate(LocalDate.of(2024, 2, 1));
        req.setLeaseType("MAIN_RESIDENCE_9Y");
        req.setDurationMonths(108);
        req.setNoticePeriodMonths(3);
        req.setMonthlyRent(new BigDecimal("850.00"));
        req.setMonthlyCharges(new BigDecimal("50.00"));
        req.setChargesType("FORFAIT");

        AddTenantRequest tr = new AddTenantRequest();
        tr.setPersonId(100L);
        tr.setRole("PRIMARY");
        req.setTenants(List.of(tr));
        return req;
    }
}
