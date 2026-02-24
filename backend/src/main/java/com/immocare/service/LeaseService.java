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
import com.immocare.exception.LeaseStatusTransitionException;
import com.immocare.model.dto.AddTenantRequest;
import com.immocare.model.dto.ChangeLeaseStatusRequest;
import com.immocare.model.dto.CreateLeaseRequest;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.dto.LeaseDTO;
import com.immocare.model.dto.LeaseIndexationDTO;
import com.immocare.model.dto.LeaseSummaryDTO;
import com.immocare.model.dto.LeaseTenantDTO;
import com.immocare.model.dto.RecordIndexationRequest;
import com.immocare.model.dto.UpdateLeaseRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Lease;
import com.immocare.model.entity.LeaseIndexationHistory;
import com.immocare.model.entity.LeaseTenant;
import com.immocare.model.entity.Person;
import com.immocare.model.enums.ChargesType;
import com.immocare.model.enums.DepositType;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.model.enums.LeaseType;
import com.immocare.model.enums.TenantRole;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseIndexationRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.LeaseTenantRepository;
import com.immocare.repository.PersonRepository;

@Service
@Transactional(readOnly = true)
public class LeaseService {

    // Default notice periods by lease type (months)
    private static final Map<String, Integer> DEFAULT_NOTICE = Map.of(
            "SHORT_TERM", 1,
            "MAIN_RESIDENCE_3Y", 3,
            "MAIN_RESIDENCE_6Y", 3,
            "MAIN_RESIDENCE_9Y", 3,
            "STUDENT", 1,
            "GLIDING", 3,
            "COMMERCIAL", 6);

    private final LeaseRepository leaseRepository;
    private final LeaseTenantRepository leaseTenantRepository;
    private final LeaseIndexationRepository indexationRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final PersonRepository personRepository;

    public LeaseService(LeaseRepository leaseRepository,
            LeaseTenantRepository leaseTenantRepository,
            LeaseIndexationRepository indexationRepository,
            HousingUnitRepository housingUnitRepository,
            PersonRepository personRepository) {
        this.leaseRepository = leaseRepository;
        this.leaseTenantRepository = leaseTenantRepository;
        this.indexationRepository = indexationRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.personRepository = personRepository;
    }

    // ---- Get ----

    public List<LeaseSummaryDTO> getByUnit(Long unitId) {
        return leaseRepository.findByHousingUnitIdOrderByStartDateDesc(unitId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public LeaseDTO getById(Long id) {
        Lease lease = findLease(id);
        return toDTO(lease);
    }

    // ---- Create ----

    @Transactional
    public LeaseDTO create(CreateLeaseRequest req, boolean activate) {
        Long unitId = req.getHousingUnitId();

        // Overlap check
        if (leaseRepository.existsByHousingUnitIdAndStatusIn(
                unitId, List.of(LeaseStatus.ACTIVE, LeaseStatus.DRAFT))) {
            throw new LeaseOverlapException(unitId);
        }

        HousingUnit unit = housingUnitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Housing unit not found: " + unitId));

        // At least one PRIMARY tenant required
        validatePrimaryTenant(req.getTenants());

        Lease lease = new Lease();
        lease.setHousingUnit(unit);
        applyRequest(lease, req);
        lease.setEndDate(lease.getStartDate().plusMonths(lease.getDurationMonths()));
        lease.setStatus(activate ? LeaseStatus.ACTIVE : LeaseStatus.DRAFT);

        Lease saved = leaseRepository.save(lease);

        // Save tenants
        if (req.getTenants() != null) {
            for (AddTenantRequest tr : req.getTenants()) {
                Person person = personRepository.findById(tr.getPersonId())
                        .orElseThrow(() -> new IllegalArgumentException("Person not found: " + tr.getPersonId()));
                LeaseTenant lt = new LeaseTenant(saved, person, TenantRole.valueOf(tr.getRole()));
                saved.getTenants().add(lt);
            }
            leaseRepository.save(saved);
        }

        return toDTO(saved);
    }

    // ---- Update ----

    @Transactional
    public LeaseDTO update(Long id, UpdateLeaseRequest req) {
        Lease lease = findLease(id);
        if (!lease.isEditable()) {
            throw new LeaseNotEditableException(id, lease.getStatus().name());
        }
        applyUpdateRequest(lease, req);
        lease.setEndDate(lease.getStartDate().plusMonths(lease.getDurationMonths()));
        return toDTO(leaseRepository.save(lease));
    }

    // ---- Status transitions ----

    @Transactional
    public LeaseDTO changeStatus(Long id, ChangeLeaseStatusRequest req) {
        Lease lease = findLease(id);
        LeaseStatus from = lease.getStatus();
        LeaseStatus to;
        try {
            to = LeaseStatus.valueOf(req.getTargetStatus());
        } catch (IllegalArgumentException e) {
            throw new LeaseStatusTransitionException(from.name(), req.getTargetStatus());
        }

        boolean valid = switch (from) {
            case DRAFT -> to == LeaseStatus.ACTIVE || to == LeaseStatus.CANCELLED;
            case ACTIVE -> to == LeaseStatus.FINISHED || to == LeaseStatus.CANCELLED;
            default -> false;
        };
        if (!valid)
            throw new LeaseStatusTransitionException(from.name(), to.name());

        // Before activating: re-check overlap (excluding self)
        if (to == LeaseStatus.ACTIVE) {
            if (leaseRepository.existsByHousingUnitIdAndStatusInAndIdNot(
                    lease.getHousingUnit().getId(), List.of(LeaseStatus.ACTIVE), id)) {
                throw new LeaseOverlapException(lease.getHousingUnit().getId());
            }
            // Must have at least one PRIMARY tenant
            long primaryCount = leaseTenantRepository.countByLeaseIdAndRole(id, TenantRole.PRIMARY);
            if (primaryCount < 1) {
                throw new IllegalStateException("Cannot activate lease without a PRIMARY tenant.");
            }
        }

        lease.setStatus(to);
        return toDTO(leaseRepository.save(lease));
    }

    // ---- Tenants ----

    @Transactional
    public LeaseDTO addTenant(Long leaseId, AddTenantRequest req) {
        Lease lease = findLease(leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        if (leaseTenantRepository.existsByLeaseIdAndPersonId(leaseId, req.getPersonId())) {
            throw new IllegalArgumentException("Person " + req.getPersonId() + " is already a tenant on this lease.");
        }

        Person person = personRepository.findById(req.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + req.getPersonId()));

        LeaseTenant lt = new LeaseTenant(lease, person, TenantRole.valueOf(req.getRole()));
        lease.getTenants().add(lt);
        return toDTO(leaseRepository.save(lease));
    }

    @Transactional
    public LeaseDTO removeTenant(Long leaseId, Long personId) {
        Lease lease = findLease(leaseId);
        if (!lease.isEditable())
            throw new LeaseNotEditableException(leaseId, lease.getStatus().name());

        LeaseTenant target = lease.getTenants().stream()
                .filter(t -> t.getPerson().getId().equals(personId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person " + personId + " is not a tenant on lease " + leaseId));

        // Prevent removing last PRIMARY
        if (target.getRole() == TenantRole.PRIMARY) {
            long primaryCount = leaseTenantRepository.countByLeaseIdAndRole(leaseId, TenantRole.PRIMARY);
            if (primaryCount <= 1) {
                throw new IllegalStateException("Cannot remove the last PRIMARY tenant from a lease.");
            }
        }

        lease.getTenants().remove(target);
        return toDTO(leaseRepository.save(lease));
    }

    // ---- Indexation ----

    @Transactional
    public LeaseDTO recordIndexation(Long leaseId, RecordIndexationRequest req) {
        Lease lease = findLease(leaseId);
        if (lease.getStatus() != LeaseStatus.ACTIVE) {
            throw new IllegalStateException("Indexation can only be recorded on an ACTIVE lease.");
        }

        // Capture old rent
        BigDecimal oldRent = lease.getMonthlyRent();

        // Create indexation history entry
        LeaseIndexationHistory history = new LeaseIndexationHistory();
        history.setLease(lease);
        history.setApplicationDate(req.getApplicationDate());
        history.setOldRent(oldRent);
        history.setNewIndexValue(req.getNewIndexValue());
        history.setNewIndexMonth(req.getNewIndexMonth());
        history.setAppliedRent(req.getAppliedRent());
        history.setNotificationDate(req.getNotificationSentDate());
        history.setNotes(req.getNotes());
        indexationRepository.save(history);

        // Update lease current rent
        lease.setMonthlyRent(req.getAppliedRent());
        return toDTO(leaseRepository.save(lease));
    }

    public List<LeaseIndexationDTO> getIndexationHistory(Long leaseId) {
        findLease(leaseId); // verify exists
        return indexationRepository.findByLeaseIdOrderByApplicationDateDesc(leaseId)
                .stream()
                .map(this::toIndexationDTO)
                .collect(Collectors.toList());
    }

    // ---- Alerts ----

    public List<LeaseAlertDTO> getAlerts() {
        List<LeaseAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Lease lease : leaseRepository.findAllActiveWithTenants()) {
            // End notice alert: today >= endDate - noticePeriodMonths
            LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
            if (!today.isBefore(noticeDeadline)) {
                alerts.add(buildAlert(lease, "END_NOTICE", noticeDeadline));
            }

            // Indexation alert
            if (lease.getIndexationAnniversaryMonth() != null) {
                int anniversaryMonth = lease.getIndexationAnniversaryMonth();
                int year = today.getYear();
                LocalDate anniversaryThisYear = LocalDate.of(year, anniversaryMonth, 1);
                LocalDate alertTrigger = anniversaryThisYear.minusDays(lease.getIndexationNoticeDays());

                if (!today.isBefore(alertTrigger)) {
                    // Check if indexation already recorded this year
                    boolean alreadyDone = indexationRepository.existsByLeaseIdAndYear(lease.getId(), year);
                    if (!alreadyDone) {
                        alerts.add(buildAlert(lease, "INDEXATION", anniversaryThisYear));
                    }
                }
            }
        }

        alerts.sort(Comparator.comparing(LeaseAlertDTO::getDeadline));
        return alerts;
    }

    // ---- Private helpers ----

    private Lease findLease(Long id) {
        return leaseRepository.findById(id)
                .orElseThrow(() -> new LeaseNotFoundException(id));
    }

    private void validatePrimaryTenant(List<AddTenantRequest> tenants) {
        if (tenants == null || tenants.isEmpty() ||
                tenants.stream().noneMatch(t -> "PRIMARY".equals(t.getRole()))) {
            throw new IllegalArgumentException("At least one PRIMARY tenant is required.");
        }
    }

    private void applyRequest(Lease lease, CreateLeaseRequest req) {
        lease.setSignatureDate(req.getSignatureDate());
        lease.setStartDate(req.getStartDate());
        lease.setLeaseType(LeaseType.valueOf(req.getLeaseType()));
        lease.setDurationMonths(req.getDurationMonths());
        lease.setNoticePeriodMonths(req.getNoticePeriodMonths() > 0 ? req.getNoticePeriodMonths()
                : DEFAULT_NOTICE.getOrDefault(req.getLeaseType(), 3));
        lease.setIndexationNoticeDays(req.getIndexationNoticeDays());
        lease.setIndexationAnniversaryMonth(req.getIndexationAnniversaryMonth());
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(
                req.getChargesType() != null ? ChargesType.valueOf(req.getChargesType()) : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setBaseIndexValue(req.getBaseIndexValue());
        lease.setBaseIndexMonth(req.getBaseIndexMonth());
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
        lease.setIndexationNoticeDays(req.getIndexationNoticeDays());
        lease.setIndexationAnniversaryMonth(req.getIndexationAnniversaryMonth());
        lease.setMonthlyRent(req.getMonthlyRent());
        lease.setMonthlyCharges(req.getMonthlyCharges() != null ? req.getMonthlyCharges() : BigDecimal.ZERO);
        lease.setChargesType(
                req.getChargesType() != null ? ChargesType.valueOf(req.getChargesType()) : ChargesType.FORFAIT);
        lease.setChargesDescription(req.getChargesDescription());
        lease.setBaseIndexValue(req.getBaseIndexValue());
        lease.setBaseIndexMonth(req.getBaseIndexMonth());
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
        dto.setIndexationNoticeDays(lease.getIndexationNoticeDays());
        dto.setIndexationAnniversaryMonth(lease.getIndexationAnniversaryMonth());
        dto.setMonthlyRent(lease.getMonthlyRent());
        dto.setMonthlyCharges(lease.getMonthlyCharges());
        dto.setChargesType(lease.getChargesType().name());
        dto.setChargesDescription(lease.getChargesDescription());
        dto.setBaseIndexValue(lease.getBaseIndexValue());
        dto.setBaseIndexMonth(lease.getBaseIndexMonth());
        dto.setRegistrationSpf(lease.getRegistrationSpf());
        dto.setRegistrationRegion(lease.getRegistrationRegion());
        dto.setDepositAmount(lease.getDepositAmount());
        dto.setDepositType(lease.getDepositType() != null ? lease.getDepositType().name() : null);
        dto.setDepositReference(lease.getDepositReference());
        dto.setTenantInsuranceConfirmed(lease.isTenantInsuranceConfirmed());
        dto.setTenantInsuranceReference(lease.getTenantInsuranceReference());
        dto.setTenantInsuranceExpiry(lease.getTenantInsuranceExpiry());
        dto.setTenants(lease.getTenants().stream().map(this::toTenantDTO).collect(Collectors.toList()));
        dto.setIndexations(lease.getIndexations().stream().map(this::toIndexationDTO).collect(Collectors.toList()));
        dto.setCreatedAt(lease.getCreatedAt());
        dto.setUpdatedAt(lease.getUpdatedAt());

        // Compute alerts
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
        dto.setChargesType(lease.getChargesType().name());
        dto.setTenantNames(lease.getTenants().stream()
                .map(t -> t.getPerson().getLastName() + " " + t.getPerson().getFirstName())
                .collect(Collectors.toList()));
        computeAlerts(lease, dto);
        return dto;
    }

    private void computeAlerts(Lease lease, LeaseDTO dto) {
        LocalDate today = LocalDate.now();
        // End notice
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        // Indexation
        if (lease.getIndexationAnniversaryMonth() != null) {
            int year = today.getYear();
            LocalDate anniversary = LocalDate.of(year, lease.getIndexationAnniversaryMonth(), 1);
            LocalDate trigger = anniversary.minusDays(lease.getIndexationNoticeDays());
            boolean alreadyDone = indexationRepository.existsByLeaseIdAndYear(lease.getId(), year);
            dto.setIndexationAlertActive(!today.isBefore(trigger) && !alreadyDone);
            dto.setIndexationAlertDate(anniversary);
        }
    }

    private void computeAlerts(Lease lease, LeaseSummaryDTO dto) {
        LocalDate today = LocalDate.now();
        LocalDate noticeDeadline = lease.getEndDate().minusMonths(lease.getNoticePeriodMonths());
        dto.setEndNoticeAlertActive(!today.isBefore(noticeDeadline));
        dto.setEndNoticeAlertDate(noticeDeadline);
        if (lease.getIndexationAnniversaryMonth() != null) {
            int year = today.getYear();
            LocalDate anniversary = LocalDate.of(year, lease.getIndexationAnniversaryMonth(), 1);
            LocalDate trigger = anniversary.minusDays(lease.getIndexationNoticeDays());
            boolean alreadyDone = indexationRepository.existsByLeaseIdAndYear(lease.getId(), year);
            dto.setIndexationAlertActive(!today.isBefore(trigger) && !alreadyDone);
            dto.setIndexationAlertDate(anniversary);
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

    private LeaseIndexationDTO toIndexationDTO(LeaseIndexationHistory h) {
        LeaseIndexationDTO dto = new LeaseIndexationDTO();
        dto.setId(h.getId());
        dto.setApplicationDate(h.getApplicationDate());
        dto.setOldRent(h.getOldRent());
        dto.setNewIndexValue(h.getNewIndexValue());
        dto.setNewIndexMonth(h.getNewIndexMonth());
        dto.setAppliedRent(h.getAppliedRent());
        dto.setNotificationDate(h.getNotificationDate());
        dto.setNotes(h.getNotes());
        dto.setCreatedAt(h.getCreatedAt());
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
