package com.immocare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateAccessDeniedException;
import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.LeaseNotEditableException;
import com.immocare.exception.LeaseNotFoundException;
import com.immocare.exception.LeaseOverlapException;
import com.immocare.model.dto.AddTenantRequest;
import com.immocare.model.dto.AdjustRentRequest;
import com.immocare.model.dto.ChangeLeaseStatusRequest;
import com.immocare.model.dto.CreateLeaseRequest;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.dto.LeaseDTO;
import com.immocare.model.dto.LeaseFilterParams;
import com.immocare.model.dto.LeaseGlobalSummaryDTO;
import com.immocare.model.dto.LeaseRentAdjustmentDTO;
import com.immocare.model.dto.LeaseSummaryDTO;
import com.immocare.model.dto.LeaseTenantDTO;
import com.immocare.model.dto.UpdateLeaseRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Lease;
import com.immocare.model.entity.LeaseRentAdjustment;
import com.immocare.model.entity.LeaseTenant;
import com.immocare.model.entity.Person;
import com.immocare.model.enums.ChargesType;
import com.immocare.model.enums.DepositType;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;
import com.immocare.model.enums.TenantRole;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRentAdjustmentRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.LeaseTenantRepository;
import com.immocare.repository.PersonRepository;
import com.immocare.repository.spec.LeaseSpecification;

/**
 * Business logic for UC011 — Manage Leases.
 * UC004_ESTATE_PLACEHOLDER Phase 3: all operations are now scoped to an estate.
 */
@Service
public class LeaseService {

    private static final int INDEXATION_NOTICE_DAYS = 30;

    private static final Map<String, Integer> DEFAULT_NOTICE = Map.of(
            "SHORT_TERM", 1,
            "MAIN_RESIDENCE_3Y", 3,
            "MAIN_RESIDENCE_6Y", 3,
            "MAIN_RESIDENCE_9Y", 3,
            "STUDENT", 1,
            "GLIDING", 3,
            "COMMERCIAL", 6);

    private final LeaseRepository leaseRepository;
    private final LeaseRentAdjustmentRepository adjustmentRepository;
    private final LeaseTenantRepository leaseTenantRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final PersonRepository personRepository;

    public LeaseService(LeaseRepository leaseRepository,
            LeaseRentAdjustmentRepository adjustmentRepository,
            LeaseTenantRepository leaseTenantRepository,
            HousingUnitRepository housingUnitRepository,
            PersonRepository personRepository) {
        this.leaseRepository = leaseRepository;
        this.adjustmentRepository = adjustmentRepository;
        this.leaseTenantRepository = leaseTenantRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.personRepository = personRepository;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<LeaseSummaryDTO> getByUnit(UUID estateId, Long unitId) {
        verifyUnitBelongsToEstate(estateId, unitId);
        return leaseRepository.findByEstateIdAndUnitId(estateId, unitId)
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    public LeaseDTO getById(UUID estateId, Long id) {
        return toDTO(findLeaseInEstate(estateId, id));
    }

    public Page<LeaseGlobalSummaryDTO> getAll(UUID estateId, LeaseFilterParams params, Pageable pageable) {
        if (params.getStatuses() == null || params.getStatuses().isEmpty()) {
            params.setStatuses(List.of(LeaseStatus.ACTIVE));
        }
        // Force estate scope in the specification
        var spec = LeaseSpecification.of(params)
                .and(LeaseSpecification.hasEstate(estateId));
        return leaseRepository.findAll(spec, pageable).map(this::toGlobalSummary);
    }

    public List<LeaseAlertDTO> getAlerts(UUID estateId) {
        return leaseRepository
                .findByEstateIdAndStatusIn(estateId, List.of(LeaseStatus.ACTIVE, LeaseStatus.DRAFT))
                .stream()
                .flatMap(lease -> {
                    List<LeaseAlertDTO> alerts = new java.util.ArrayList<>();
                    LocalDate today = LocalDate.now();
                    LocalDate noticeDeadline = lease.getEndDate()
                            .minusMonths(lease.getNoticePeriodMonths());
                    if (!today.isBefore(noticeDeadline)) {
                        alerts.add(buildAlert(lease, "END_NOTICE", noticeDeadline));
                    }
                    addIndexationAlert(lease, today, alerts);
                    return alerts.stream();
                })
                .collect(Collectors.toList());
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public LeaseDTO create(UUID estateId, Long unitId, CreateLeaseRequest req, boolean activate) {
        verifyUnitBelongsToEstate(estateId, unitId);

        if (leaseRepository.existsByEstateIdAndUnitIdAndStatusIn(
                estateId, unitId, List.of(LeaseStatus.ACTIVE, LeaseStatus.DRAFT))) {
            throw new LeaseOverlapException(unitId);
        }

        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new HousingUnitNotFoundException(unitId));

        validatePrimaryTenant(req.getTenants());

        Lease lease = new Lease();
        lease.setHousingUnit(unit);
        applyRequest(lease, req);
        lease.setEndDate(req.getEndDate());
        lease.setStatus(activate ? LeaseStatus.ACTIVE : LeaseStatus.DRAFT);

        Lease saved = leaseRepository.save(lease);

        for (AddTenantRequest tr : req.getTenants()) {
            Person person = personRepository.findById(tr.getPersonId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Person not found: " + tr.getPersonId()));
            // BR: tenant must belong to the same estate
            if (!personRepository.existsByEstateIdAndId(estateId, tr.getPersonId())) {
                throw new EstateAccessDeniedException();
            }
            leaseTenantRepository.save(new LeaseTenant(saved, person, TenantRole.valueOf(tr.getRole())));
        }

        return toDTO(leaseRepository.findById(saved.getId()).orElseThrow());
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public LeaseDTO update(UUID estateId, Long id, UpdateLeaseRequest req) {
        Lease lease = findLeaseInEstate(estateId, id);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(id, lease.getStatus().name());
        applyUpdateRequest(lease, req);
        lease.setEndDate(req.getEndDate());
        return toDTO(leaseRepository.save(lease));
    }

    // ─── Status ───────────────────────────────────────────────────────────────

    @Transactional
    public LeaseDTO changeStatus(UUID estateId, Long id, ChangeLeaseStatusRequest req) {
        Lease lease = findLeaseInEstate(estateId, id);
        LeaseStatus to = LeaseStatus.valueOf(req.getTargetStatus());
        validateTransition(lease.getStatus(), to, id, lease);
        lease.setStatus(to);
        return toDTO(leaseRepository.save(lease));
    }

    // ─── Tenants ──────────────────────────────────────────────────────────────

    @Transactional
    public LeaseDTO addTenant(UUID estateId, Long leaseId, AddTenantRequest req) {
        Lease lease = findLeaseInEstate(estateId, leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        if (leaseTenantRepository.existsByLeaseIdAndPersonId(leaseId, req.getPersonId())) {
            throw new IllegalArgumentException(
                    "Person " + req.getPersonId() + " is already a tenant on this lease.");
        }

        // BR: tenant must belong to the same estate
        if (!personRepository.existsByEstateIdAndId(estateId, req.getPersonId())) {
            throw new EstateAccessDeniedException();
        }

        Person person = personRepository.findById(req.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + req.getPersonId()));

        lease.getTenants().add(new LeaseTenant(lease, person, TenantRole.valueOf(req.getRole())));
        return toDTO(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseDTO removeTenant(UUID estateId, Long leaseId, Long personId) {
        Lease lease = findLeaseInEstate(estateId, leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        LeaseTenant target = lease.getTenants().stream()
                .filter(t -> t.getPerson().getId().equals(personId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person " + personId + " is not a tenant on lease " + leaseId));

        if (target.getRole() == TenantRole.PRIMARY) {
            long count = leaseTenantRepository.countByLeaseIdAndRole(leaseId, TenantRole.PRIMARY);
            if (count <= 1)
                throw new IllegalStateException("Cannot remove the last PRIMARY tenant.");
        }

        lease.getTenants().remove(target);
        return toDTO(leaseRepository.save(lease));
    }

    // ─── Rent adjustments ─────────────────────────────────────────────────────

    @Transactional
    public LeaseDTO adjustRent(UUID estateId, Long leaseId, AdjustRentRequest req) {
        Lease lease = findLeaseInEstate(estateId, leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        String field = req.getField().toUpperCase();
        if (!field.equals("RENT") && !field.equals("CHARGES")) {
            throw new IllegalArgumentException("field must be RENT or CHARGES");
        }

        BigDecimal oldValue = field.equals("RENT") ? lease.getMonthlyRent() : lease.getMonthlyCharges();

        LeaseRentAdjustment adj = new LeaseRentAdjustment();
        adj.setLease(lease);
        adj.setField(field);
        adj.setOldValue(oldValue);
        adj.setNewValue(req.getNewValue());
        adj.setReason(req.getReason());
        adj.setEffectiveDate(req.getEffectiveDate());
        adjustmentRepository.save(adj);

        if (field.equals("RENT"))
            lease.setMonthlyRent(req.getNewValue());
        else
            lease.setMonthlyCharges(req.getNewValue());

        return toDTO(leaseRepository.save(lease));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /**
     * Loads a lease and verifies it belongs to the given estate.
     * Throws {@link LeaseNotFoundException} if the lease does not exist,
     * or {@link EstateAccessDeniedException} if it belongs to another estate.
     */
    private Lease findLeaseInEstate(UUID estateId, Long leaseId) {
        return leaseRepository.findByEstateIdAndId(estateId, leaseId)
                .orElseGet(() -> {
                    // Distinguish "not found at all" from "wrong estate"
                    if (!leaseRepository.existsById(leaseId)) {
                        throw new LeaseNotFoundException(leaseId);
                    }
                    throw new EstateAccessDeniedException();
                });
    }

    /**
     * Verifies that the housing unit belongs to the given estate.
     */
    private void verifyUnitBelongsToEstate(UUID estateId, Long unitId) {
        if (!housingUnitRepository.existsById(unitId)) {
            throw new HousingUnitNotFoundException(unitId);
        }
        if (!housingUnitRepository.existsByBuilding_Estate_IdAndId(estateId, unitId)) {
            throw new EstateAccessDeniedException();
        }
    }

    private void validateTransition(LeaseStatus from, LeaseStatus to, Long id, Lease lease) {
        boolean valid = switch (from) {
            case DRAFT -> to == LeaseStatus.ACTIVE || to == LeaseStatus.CANCELLED;
            case ACTIVE -> to == LeaseStatus.FINISHED || to == LeaseStatus.CANCELLED;
            default -> false;
        };
        if (!valid)
            throw new IllegalStateException("Cannot transition from " + from + " to " + to);
        if (to == LeaseStatus.ACTIVE && lease.getStartDate() == null) {
            throw new IllegalStateException("Cannot activate a lease without a start date.");
        }
    }

    private void validatePrimaryTenant(List<AddTenantRequest> tenants) {
        if (tenants == null || tenants.stream().noneMatch(t -> "PRIMARY".equals(t.getRole()))) {
            throw new IllegalArgumentException("At least one PRIMARY tenant is required.");
        }
    }

    private void applyRequest(Lease lease, CreateLeaseRequest req) {
        lease.setSignatureDate(req.getSignatureDate());
        lease.setStartDate(req.getStartDate());
        lease.setLeaseType(LeaseType.valueOf(req.getLeaseType()));
        lease.setDurationMonths(req.getDurationMonths());
        lease.setNoticePeriodMonths(req.getNoticePeriodMonths() > 0
                ? req.getNoticePeriodMonths()
                : DEFAULT_NOTICE.getOrDefault(req.getLeaseType(), 3));
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(req.getChargesType() != null
                ? ChargesType.valueOf(req.getChargesType())
                : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setRegistrationSpf(req.getRegistrationSpf());
        lease.setRegistrationRegion(req.getRegistrationRegion());
        lease.setRegistrationInventorySpf(req.getRegistrationInventorySpf());
        lease.setRegistrationInventoryRegion(req.getRegistrationInventoryRegion());
        lease.setDepositAmount(req.getDepositAmount());
        lease.setDepositType(req.getDepositType() != null ? DepositType.valueOf(req.getDepositType()) : null);
        lease.setDepositReference(req.getDepositReference());
        lease.setTenantInsuranceConfirmed(req.isTenantInsuranceConfirmed());
        lease.setTenantInsuranceReference(req.getTenantInsuranceReference());
        lease.setTenantInsuranceExpiry(req.getTenantInsuranceExpiry());
    }

    private void applyUpdateRequest(Lease lease, UpdateLeaseRequest req) {
        lease.setSignatureDate(req.getSignatureDate());
        lease.setStartDate(req.getStartDate());
        lease.setLeaseType(LeaseType.valueOf(req.getLeaseType()));
        lease.setDurationMonths(req.getDurationMonths());
        lease.setNoticePeriodMonths(req.getNoticePeriodMonths() > 0
                ? req.getNoticePeriodMonths()
                : DEFAULT_NOTICE.getOrDefault(req.getLeaseType(), 3));
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(req.getChargesType() != null
                ? ChargesType.valueOf(req.getChargesType())
                : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setRegistrationSpf(req.getRegistrationSpf());
        lease.setRegistrationRegion(req.getRegistrationRegion());
        lease.setRegistrationInventorySpf(req.getRegistrationInventorySpf());
        lease.setRegistrationInventoryRegion(req.getRegistrationInventoryRegion());
        lease.setDepositAmount(req.getDepositAmount());
        lease.setDepositType(req.getDepositType() != null ? DepositType.valueOf(req.getDepositType()) : null);
        lease.setDepositReference(req.getDepositReference());
        lease.setTenantInsuranceConfirmed(req.isTenantInsuranceConfirmed());
        lease.setTenantInsuranceReference(req.getTenantInsuranceReference());
        lease.setTenantInsuranceExpiry(req.getTenantInsuranceExpiry());
    }

    private LeaseDTO toDTO(Lease lease) {
        LeaseDTO dto = new LeaseDTO();
        dto.setId(lease.getId());
        dto.setHousingUnitId(lease.getHousingUnit().getId());
        dto.setHousingUnitNumber(lease.getHousingUnit().getUnitNumber());
        dto.setBuildingId(lease.getHousingUnit().getBuilding().getId());
        dto.setBuildingName(lease.getHousingUnit().getBuilding().getName());
        dto.setStatus(lease.getStatus().name());
        dto.setSignatureDate(lease.getSignatureDate());
        dto.setStartDate(lease.getStartDate());
        dto.setEndDate(lease.getEndDate());
        dto.setLeaseType(lease.getLeaseType().name());
        dto.setDurationMonths(lease.getDurationMonths());
        dto.setNoticePeriodMonths(lease.getNoticePeriodMonths());
        dto.setMonthlyRent(lease.getMonthlyRent());
        dto.setMonthlyCharges(lease.getMonthlyCharges());
        dto.setTotalRent(lease.getMonthlyRent().add(lease.getMonthlyCharges()));
        dto.setChargesType(lease.getChargesType().name());
        dto.setChargesDescription(lease.getChargesDescription());
        dto.setRegistrationSpf(lease.getRegistrationSpf());
        dto.setRegistrationRegion(lease.getRegistrationRegion());
        dto.setRegistrationInventorySpf(lease.getRegistrationInventorySpf());
        dto.setRegistrationInventoryRegion(lease.getRegistrationInventoryRegion());
        dto.setDepositAmount(lease.getDepositAmount());
        dto.setDepositType(lease.getDepositType() != null ? lease.getDepositType().name() : null);
        dto.setDepositReference(lease.getDepositReference());
        dto.setTenantInsuranceConfirmed(lease.isTenantInsuranceConfirmed());
        dto.setTenantInsuranceReference(lease.getTenantInsuranceReference());
        dto.setTenantInsuranceExpiry(lease.getTenantInsuranceExpiry());
        dto.setTenants(lease.getTenants().stream().map(this::toTenantDTO).collect(Collectors.toList()));
        dto.setRentAdjustments(
                lease.getRentAdjustments().stream().map(this::toAdjustmentDTO).collect(Collectors.toList()));
        dto.setCreatedAt(lease.getCreatedAt());
        dto.setUpdatedAt(lease.getUpdatedAt());
        computeAlerts(lease, dto);
        return dto;
    }

    private LeaseSummaryDTO toSummary(Lease lease) {
        LeaseSummaryDTO dto = new LeaseSummaryDTO();
        dto.setId(lease.getId());
        dto.setStatus(lease.getStatus().name());
        dto.setLeaseType(lease.getLeaseType().name());
        dto.setStartDate(lease.getStartDate());
        dto.setEndDate(lease.getEndDate());
        dto.setMonthlyRent(lease.getMonthlyRent());
        dto.setMonthlyCharges(lease.getMonthlyCharges());
        dto.setTotalRent(lease.getMonthlyRent().add(lease.getMonthlyCharges()));
        dto.setChargesType(lease.getChargesType().name());
        dto.setTenantNames(lease.getTenants().stream()
                .map(t -> t.getPerson().getLastName() + " " + t.getPerson().getFirstName())
                .collect(Collectors.toList()));
        computeAlerts(lease, dto);
        return dto;
    }

    private LeaseGlobalSummaryDTO toGlobalSummary(Lease lease) {
        LeaseGlobalSummaryDTO dto = new LeaseGlobalSummaryDTO();
        dto.setId(lease.getId());
        dto.setStatus(lease.getStatus().name());
        dto.setLeaseType(lease.getLeaseType().name());
        HousingUnit unit = lease.getHousingUnit();
        dto.setHousingUnitId(unit.getId());
        dto.setHousingUnitNumber(unit.getUnitNumber());
        dto.setBuildingId(unit.getBuilding().getId());
        dto.setBuildingName(unit.getBuilding().getName());
        dto.setStartDate(lease.getStartDate());
        dto.setEndDate(lease.getEndDate());
        dto.setMonthlyRent(lease.getMonthlyRent());
        dto.setMonthlyCharges(lease.getMonthlyCharges());
        dto.setTotalRent(lease.getMonthlyRent().add(lease.getMonthlyCharges()));
        dto.setChargesType(lease.getChargesType().name());
        dto.setTenantNames(lease.getTenants().stream()
                .filter(t -> t.getRole() == TenantRole.PRIMARY || t.getRole() == TenantRole.CO_TENANT)
                .map(t -> t.getPerson().getLastName() + " " + t.getPerson().getFirstName())
                .collect(Collectors.toList()));
        computeAlerts(dto, lease);
        return dto;
    }

    private void computeAlerts(Lease lease, LeaseDTO dto) {
        LocalDate today = LocalDate.now();
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        setIndexationAlert(lease, today, dto::setIndexationAlertActive, dto::setIndexationAlertDate);
    }

    private void computeAlerts(Lease lease, LeaseSummaryDTO dto) {
        LocalDate today = LocalDate.now();
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        setIndexationAlert(lease, today, dto::setIndexationAlertActive, dto::setIndexationAlertDate);
    }

    private void computeAlerts(LeaseGlobalSummaryDTO dto, Lease lease) {
        if (lease.getStatus() != LeaseStatus.ACTIVE || lease.getStartDate() == null)
            return;
        LocalDate today = LocalDate.now();
        LocalDate anniversary = lease.getStartDate().withYear(today.getYear());
        if (anniversary.isBefore(today))
            anniversary = anniversary.plusYears(1);
        long daysToAnniversary = java.time.temporal.ChronoUnit.DAYS.between(today, anniversary);
        boolean indexedThisYear = adjustmentRepository.existsRentAdjustmentForYear(
                lease.getId(), today.getYear());
        if (daysToAnniversary <= INDEXATION_NOTICE_DAYS && !indexedThisYear) {
            dto.setIndexationAlertActive(true);
            dto.setIndexationAlertDate(anniversary);
        }
        if (lease.getEndDate() != null) {
            LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
            if (!today.isAfter(lease.getEndDate()) && !today.isBefore(noticeDeadline)) {
                dto.setEndNoticeAlertActive(true);
                dto.setEndNoticeAlertDate(noticeDeadline);
            }
        }
    }

    private void setIndexationAlert(Lease lease, LocalDate today,
            java.util.function.Consumer<Boolean> setActive,
            java.util.function.Consumer<LocalDate> setDate) {
        if (lease.getStartDate() == null)
            return;
        LocalDate anniversary = LocalDate.of(today.getYear(),
                lease.getStartDate().getMonthValue(),
                lease.getStartDate().getDayOfMonth());
        if (anniversary.isBefore(today))
            anniversary = anniversary.plusYears(1);
        LocalDate trigger = anniversary.minusDays(INDEXATION_NOTICE_DAYS);
        if (!today.isBefore(trigger)) {
            boolean done = adjustmentRepository.existsRentAdjustmentForYear(
                    lease.getId(), anniversary.getYear());
            setActive.accept(!done);
            setDate.accept(anniversary);
        }
    }

    private void addIndexationAlert(Lease lease, LocalDate today, List<LeaseAlertDTO> alerts) {
        if (lease.getStartDate() == null)
            return;
        LocalDate anniversary = LocalDate.of(today.getYear(),
                lease.getStartDate().getMonthValue(),
                lease.getStartDate().getDayOfMonth());
        if (anniversary.isBefore(today))
            anniversary = anniversary.plusYears(1);
        LocalDate trigger = anniversary.minusDays(INDEXATION_NOTICE_DAYS);
        if (!today.isBefore(trigger)) {
            boolean done = adjustmentRepository.existsRentAdjustmentForYear(
                    lease.getId(), anniversary.getYear());
            if (!done)
                alerts.add(buildAlert(lease, "INDEXATION", anniversary));
        }
    }

    private LeaseTenantDTO toTenantDTO(LeaseTenant lt) {
        LeaseTenantDTO dto = new LeaseTenantDTO();
        dto.setPersonId(lt.getPerson().getId());
        dto.setLastName(lt.getPerson().getLastName());
        dto.setFirstName(lt.getPerson().getFirstName());
        dto.setEmail(lt.getPerson().getEmail());
        dto.setGsm(lt.getPerson().getGsm());
        dto.setRole(lt.getRole().name());
        return dto;
    }

    private LeaseRentAdjustmentDTO toAdjustmentDTO(LeaseRentAdjustment a) {
        LeaseRentAdjustmentDTO dto = new LeaseRentAdjustmentDTO();
        dto.setId(a.getId());
        dto.setField(a.getField());
        dto.setOldValue(a.getOldValue());
        dto.setNewValue(a.getNewValue());
        dto.setReason(a.getReason());
        dto.setEffectiveDate(a.getEffectiveDate());
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    private LeaseAlertDTO buildAlert(Lease lease, String type, LocalDate deadline) {
        LeaseAlertDTO alert = new LeaseAlertDTO();
        alert.setLeaseId(lease.getId());
        alert.setHousingUnitId(lease.getHousingUnit().getId());
        alert.setHousingUnitNumber(lease.getHousingUnit().getUnitNumber());
        alert.setBuildingName(lease.getHousingUnit().getBuilding().getName());
        alert.setAlertType(type);
        alert.setDeadline(deadline);
        alert.setTenantNames(lease.getTenants().stream()
                .filter(t -> t.getRole() == TenantRole.PRIMARY)
                .map(t -> t.getPerson().getLastName() + " " + t.getPerson().getFirstName())
                .collect(Collectors.toList()));
        return alert;
    }
}
