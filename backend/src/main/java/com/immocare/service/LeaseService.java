package com.immocare.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.LeaseNotEditableException;
import com.immocare.exception.LeaseNotFoundException;
import com.immocare.exception.LeaseOverlapException;
import com.immocare.model.dto.AddTenantRequest;
import com.immocare.model.dto.AdjustRentRequest;
import com.immocare.model.dto.ChangeLeaseStatusRequest;
import com.immocare.model.dto.CreateLeaseRequest;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.dto.LeaseDTO;
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

@Service
public class LeaseService {

    // 30 days notice before indexation anniversary by default
    private static final int INDEXATION_NOTICE_DAYS = 30;

    private static final Map<String, Integer> DEFAULT_NOTICE = Map.of(
            "SHORT_TERM", 1, "MAIN_RESIDENCE_3Y", 3, "MAIN_RESIDENCE_6Y", 3,
            "MAIN_RESIDENCE_9Y", 3, "STUDENT", 1, "GLIDING", 3, "COMMERCIAL", 6);

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

    // ---- Get ----

    public List<LeaseSummaryDTO> getByUnit(Long unitId) {
        return leaseRepository.findByHousingUnitIdOrderByStartDateDesc(unitId)
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    public LeaseDTO getById(Long id) {
        return toDTO(findLease(id));
    }

    // ---- Create ----

    @Transactional
    public LeaseDTO create(CreateLeaseRequest req, boolean activate) {
        Long unitId = req.getHousingUnitId();
        if (leaseRepository.existsByHousingUnitIdAndStatusIn(unitId, List.of(LeaseStatus.ACTIVE, LeaseStatus.DRAFT)))
            throw new LeaseOverlapException(unitId);

        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Housing unit not found: " + unitId));

        validatePrimaryTenant(req.getTenants());

        Lease lease = new Lease();
        lease.setHousingUnit(unit);
        applyRequest(lease, req);
        lease.setEndDate(lease.getStartDate().plusMonths(lease.getDurationMonths()));
        lease.setStatus(activate ? LeaseStatus.ACTIVE : LeaseStatus.DRAFT);

        Lease saved = leaseRepository.save(lease);

        for (AddTenantRequest tr : req.getTenants()) {
            Person person = personRepository.findById(tr.getPersonId())
                    .orElseThrow(() -> new IllegalArgumentException("Person not found: " + tr.getPersonId()));
            leaseTenantRepository.save(new LeaseTenant(saved, person, TenantRole.valueOf(tr.getRole())));
        }

        return toDTO(leaseRepository.findById(saved.getId()).orElseThrow());
    }

    // ---- Update ----

    @Transactional
    public LeaseDTO update(Long id, UpdateLeaseRequest req) {
        Lease lease = findLease(id);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(id, lease.getStatus().name());
        applyUpdateRequest(lease, req);
        lease.setEndDate(lease.getStartDate().plusMonths(lease.getDurationMonths()));
        return toDTO(leaseRepository.save(lease));
    }

    // ---- Status ----

    @Transactional
    public LeaseDTO changeStatus(Long id, ChangeLeaseStatusRequest req) {
        Lease lease = findLease(id);
        LeaseStatus to = LeaseStatus.valueOf(req.getTargetStatus());
        validateTransition(lease.getStatus(), to, id, lease);
        lease.setStatus(to);
        return toDTO(leaseRepository.save(lease));
    }

    private void validateTransition(LeaseStatus from, LeaseStatus to, Long id, Lease lease) {
        boolean valid = switch (from) {
            case DRAFT -> to == LeaseStatus.ACTIVE || to == LeaseStatus.CANCELLED;
            case ACTIVE -> to == LeaseStatus.FINISHED || to == LeaseStatus.CANCELLED;
            default -> false;
        };
        if (!valid)
            throw new IllegalStateException("Cannot transition from " + from + " to " + to);
        if (to == LeaseStatus.ACTIVE && lease.getStartDate() == null)
            throw new IllegalStateException("Cannot activate a lease without a start date.");
    }

    // ---- Tenants ----

    @Transactional
    public LeaseDTO addTenant(Long leaseId, AddTenantRequest req) {
        Lease lease = findLease(leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());
        if (leaseTenantRepository.existsByLeaseIdAndPersonId(leaseId, req.getPersonId()))
            throw new IllegalArgumentException("Person " + req.getPersonId() + " is already a tenant on this lease.");
        Person person = personRepository.findById(req.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + req.getPersonId()));
        lease.getTenants().add(new LeaseTenant(lease, person, TenantRole.valueOf(req.getRole())));
        return toDTO(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseDTO removeTenant(Long leaseId, Long personId) {
        Lease lease = findLease(leaseId);
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

    // ---- Rent Adjustments ----

    @Transactional
    public LeaseDTO adjustRent(Long leaseId, AdjustRentRequest req) {
        Lease lease = findLease(leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        String field = req.getField().toUpperCase();
        if (!field.equals("RENT") && !field.equals("CHARGES"))
            throw new IllegalArgumentException("field must be RENT or CHARGES");

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

    // ---- Alerts ----

    public List<LeaseAlertDTO> getAlerts() {
        List<LeaseAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Lease lease : leaseRepository.findAllActiveWithTenants()) {
            // End notice alert
            LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
            if (!today.isBefore(noticeDeadline))
                alerts.add(buildAlert(lease, "END_NOTICE", noticeDeadline));

            // Indexation alert: anniversary of startDate each year
            if (lease.getStartDate() != null) {
                int anniversaryMonth = lease.getStartDate().getMonthValue();
                int anniversaryDay = lease.getStartDate().getDayOfMonth();
                int year = today.getYear();
                LocalDate anniversaryThisYear = LocalDate.of(year, anniversaryMonth, anniversaryDay);
                // If anniversary already passed this year, look at next year
                if (anniversaryThisYear.isBefore(today.minusDays(INDEXATION_NOTICE_DAYS))) {
                    anniversaryThisYear = anniversaryThisYear.plusYears(1);
                    year++;
                }
                LocalDate alertTrigger = anniversaryThisYear.minusDays(INDEXATION_NOTICE_DAYS);
                if (!today.isBefore(alertTrigger)) {
                    boolean alreadyDone = adjustmentRepository.existsRentAdjustmentForYear(lease.getId(), year);
                    if (!alreadyDone)
                        alerts.add(buildAlert(lease, "INDEXATION", anniversaryThisYear));
                }
            }
        }
        alerts.sort(Comparator.comparing(LeaseAlertDTO::getDeadline));
        return alerts;
    }

    // ---- Private helpers ----

    private Lease findLease(Long id) {
        return leaseRepository.findById(id).orElseThrow(() -> new LeaseNotFoundException(id));
    }

    private void validatePrimaryTenant(List<AddTenantRequest> tenants) {
        if (tenants == null || tenants.isEmpty() ||
                tenants.stream().noneMatch(t -> "PRIMARY".equals(t.getRole())))
            throw new IllegalArgumentException("At least one PRIMARY tenant is required.");
    }

    private void applyRequest(Lease lease, CreateLeaseRequest req) {
        lease.setSignatureDate(req.getSignatureDate());
        lease.setStartDate(req.getStartDate());
        lease.setLeaseType(LeaseType.valueOf(req.getLeaseType()));
        lease.setDurationMonths(req.getDurationMonths());
        lease.setNoticePeriodMonths(req.getNoticePeriodMonths() > 0 ? req.getNoticePeriodMonths()
                : DEFAULT_NOTICE.getOrDefault(req.getLeaseType(), 3));
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(
                req.getChargesType() != null ? ChargesType.valueOf(req.getChargesType()) : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setRegistrationSpf(req.getRegistrationSpf());
        lease.setRegistrationRegion(req.getRegistrationRegion());
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
        lease.setNoticePeriodMonths(req.getNoticePeriodMonths() > 0 ? req.getNoticePeriodMonths()
                : DEFAULT_NOTICE.getOrDefault(req.getLeaseType(), 3));
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(
                req.getChargesType() != null ? ChargesType.valueOf(req.getChargesType()) : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setRegistrationSpf(req.getRegistrationSpf());
        lease.setRegistrationRegion(req.getRegistrationRegion());
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

    private void computeAlerts(Lease lease, LeaseDTO dto) {
        LocalDate today = LocalDate.now();
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        setIndexationAlert(lease, today,
                dto::setIndexationAlertActive, dto::setIndexationAlertDate);
    }

    private void computeAlerts(Lease lease, LeaseSummaryDTO dto) {
        LocalDate today = LocalDate.now();
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        setIndexationAlert(lease, today,
                dto::setIndexationAlertActive, dto::setIndexationAlertDate);
    }

    private void setIndexationAlert(Lease lease, LocalDate today,
            java.util.function.Consumer<Boolean> setActive,
            java.util.function.Consumer<LocalDate> setDate) {
        if (lease.getStartDate() == null)
            return;
        int month = lease.getStartDate().getMonthValue();
        int day = lease.getStartDate().getDayOfMonth();
        int year = today.getYear();
        LocalDate anniversary = LocalDate.of(year, month, day);
        if (anniversary.isBefore(today))
            anniversary = anniversary.plusYears(1);
        LocalDate trigger = anniversary.minusDays(INDEXATION_NOTICE_DAYS);
        if (!today.isBefore(trigger)) {
            boolean done = adjustmentRepository.existsRentAdjustmentForYear(lease.getId(), anniversary.getYear());
            setActive.accept(!done);
            setDate.accept(anniversary);
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
