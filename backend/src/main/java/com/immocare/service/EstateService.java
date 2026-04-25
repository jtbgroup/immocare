package com.immocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.EstateHasBuildingsException;
import com.immocare.exception.EstateLastManagerException;
import com.immocare.exception.EstateMemberAlreadyExistsException;
import com.immocare.exception.EstateMemberNotFoundException;
import com.immocare.exception.EstateNameTakenException;
import com.immocare.exception.EstateNotFoundException;
import com.immocare.exception.EstateSelfOperationException;
import com.immocare.exception.NoManagerInMembersException;
import com.immocare.exception.UserNotFoundException;
import com.immocare.model.dto.EstateDTOs.AddEstateMemberRequest;
import com.immocare.model.dto.EstateDTOs.CreateEstateRequest;
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstateDTOs.EstateDashboardDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberInput;
import com.immocare.model.dto.EstateDTOs.EstatePendingAlertsDTO;
import com.immocare.model.dto.EstateDTOs.EstateSummaryDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.dto.EstateDTOs.UpdateEstateRequest;
import com.immocare.model.dto.EstatePlatformConfigDTOs;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.model.enums.LeaseStatus;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.FireExtinguisherRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.UserRepository;

/**
 * Service for Estate management.
 *
 * Key changes vs Phase 1:
 * - {@link #createEstate} now accepts a {@code members} list (replacing the single
 *   {@code firstManagerId}) and enforces BR-UC003-02 (at least one MANAGER).
 * - {@link #updateEstateByManager} added for estate-scoped MANAGER access.
 */
@Service
@Transactional(readOnly = true)
public class EstateService {

    private final EstateRepository estateRepository;
    private final EstateMemberRepository estateMemberRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;
    private final LeaseRepository leaseRepository;
    private final BoilerRepository boilerRepository;
    private final FireExtinguisherRepository fireExtinguisherRepository;
    private final LeaseService leaseService;
    private final PlatformConfigService platformConfigService;
    private final BoilerServiceValidityRuleService validityRuleService;

    public EstateService(EstateRepository estateRepository,
            EstateMemberRepository estateMemberRepository,
            UserRepository userRepository,
            BuildingRepository buildingRepository,
            HousingUnitRepository housingUnitRepository,
            LeaseRepository leaseRepository,
            BoilerRepository boilerRepository,
            FireExtinguisherRepository fireExtinguisherRepository,
            LeaseService leaseService,
            PlatformConfigService platformConfigService,
            BoilerServiceValidityRuleService validityRuleService) {
        this.estateRepository = estateRepository;
        this.estateMemberRepository = estateMemberRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
        this.leaseRepository = leaseRepository;
        this.boilerRepository = boilerRepository;
        this.fireExtinguisherRepository = fireExtinguisherRepository;
        this.leaseService = leaseService;
        this.platformConfigService = platformConfigService;
        this.validityRuleService = validityRuleService;
    }

    // ─── Admin queries ────────────────────────────────────────────────────────

    public Page<EstateDTO> getAllEstates(String search, Pageable pageable) {
        Page<Estate> page = (search != null && !search.isBlank())
                ? estateRepository.searchByName(search, pageable)
                : estateRepository.findAllByOrderByNameAsc(pageable);
        return page.map(this::toDTO);
    }

    public EstateDTO getEstateById(UUID id) {
        return toDTO(findOrThrow(id));
    }

    // ─── User queries ─────────────────────────────────────────────────────────

    public List<EstateSummaryDTO> getMyEstates(Long userId, boolean isPlatformAdmin) {
        if (isPlatformAdmin) {
            return estateRepository.findAll()
                    .stream()
                    .map(e -> toSummaryDTO(e, null))
                    .toList();
        }
        return estateMemberRepository.findByUserId(userId)
                .stream()
                .map(m -> toSummaryDTO(m.getEstate(), m.getRole()))
                .toList();
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    public EstateDashboardDTO getDashboard(UUID estateId) {
        Estate estate = findOrThrow(estateId);

        int totalBuildings = (int) buildingRepository.countByEstateId(estateId);
        int totalUnits = (int) housingUnitRepository.countByBuilding_Estate_Id(estateId);
        int activeLeases = (int) leaseRepository
                .countByHousingUnit_Building_Estate_IdAndStatus(estateId, LeaseStatus.ACTIVE);

        int boilerAlerts = computeBoilerAlerts(estateId);
        int fireExtAlerts = computeFireExtinguisherAlerts(estateId);
        int leaseEndAlerts = computeLeaseEndAlerts(estateId);
        int indexationAlerts = computeIndexationAlerts(estateId);

        return new EstateDashboardDTO(
                estate.getId(),
                estate.getName(),
                totalBuildings,
                totalUnits,
                activeLeases,
                new EstatePendingAlertsDTO(boilerAlerts, fireExtAlerts, leaseEndAlerts, indexationAlerts));
    }

    // ─── Member management ────────────────────────────────────────────────────

    public List<EstateMemberDTO> getMembers(UUID estateId) {
        findOrThrow(estateId);
        return estateMemberRepository.findByEstateIdOrderByUserUsernameAsc(estateId)
                .stream()
                .map(this::toMemberDTO)
                .toList();
    }

    @Transactional
    public EstateMemberDTO addMember(UUID estateId, AddEstateMemberRequest req, Long currentUserId) {
        Estate estate = findOrThrow(estateId);
        AppUser user = userRepository.findById(req.userId())
                .orElseThrow(() -> new UserNotFoundException(req.userId()));

        if (estateMemberRepository.existsByEstateIdAndUserId(estateId, req.userId())) {
            throw new EstateMemberAlreadyExistsException();
        }

        EstateMember member = new EstateMember();
        member.setEstate(estate);
        member.setUser(user);
        member.setRole(req.role());
        estateMemberRepository.save(member);
        return toMemberDTO(member);
    }

    @Transactional
    public EstateMemberDTO updateMemberRole(UUID estateId, Long userId,
            UpdateEstateMemberRoleRequest req, Long currentUserId) {
        findOrThrow(estateId);

        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot change your own role");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        if (member.getRole() == EstateRole.MANAGER && req.role() != EstateRole.MANAGER) {
            long managerCount = estateMemberRepository.countByEstateIdAndRole(estateId, "MANAGER");
            if (managerCount <= 1) {
                throw new EstateLastManagerException();
            }
        }

        member.setRole(req.role());
        estateMemberRepository.save(member);
        return toMemberDTO(member);
    }

    @Transactional
    public void removeMember(UUID estateId, Long userId, Long currentUserId) {
        findOrThrow(estateId);

        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot remove yourself from an estate");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        if (member.getRole() == EstateRole.MANAGER) {
            long managerCount = estateMemberRepository.countByEstateIdAndRole(estateId, "MANAGER");
            if (managerCount <= 1) {
                throw new EstateLastManagerException();
            }
        }

        estateMemberRepository.delete(member);
    }

    // ─── Admin mutations ──────────────────────────────────────────────────────

    /**
     * Creates a new estate and bulk-adds the provided members in the same transaction.
     *
     * Business rules enforced:
     * <ul>
     *   <li>BR-UC003-01: estate name must be unique (case-insensitive)</li>
     *   <li>BR-UC003-02: if a members list is provided and non-empty, it must
     *       contain at least one entry with role MANAGER</li>
     *   <li>Duplicate userIds within the members list are rejected</li>
     * </ul>
     */
    @Transactional
    public EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId) {
        if (estateRepository.existsByNameIgnoreCase(req.name())) {
            throw new EstateNameTakenException(req.name());
        }

        // BR-UC003-02: if members are provided, at least one must be MANAGER
        List<EstateMemberInput> members = req.members() != null ? req.members() : List.of();
        if (!members.isEmpty()) {
            boolean hasManager = members.stream()
                    .anyMatch(m -> m.role() == EstateRole.MANAGER);
            if (!hasManager) {
                throw new NoManagerInMembersException();
            }
        }

        AppUser createdBy = userRepository.findById(createdByUserId).orElse(null);

        Estate estate = new Estate();
        estate.setName(req.name());
        estate.setDescription(req.description());
        estate.setCreatedBy(createdBy);
        Estate saved = estateRepository.save(estate);

        // Seed default platform config and boiler validity rule
        platformConfigService.seedDefaultConfig(saved, EstatePlatformConfigDTOs.DEFAULT_CONFIG);
        validityRuleService.seedDefaultRule(saved);

        // Bulk-add members preserving order; detect duplicates
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        for (EstateMemberInput input : members) {
            if (!seen.add(input.userId())) {
                // Duplicate userId in the request — skip silently (idempotent)
                continue;
            }
            AppUser user = userRepository.findById(input.userId())
                    .orElseThrow(() -> new UserNotFoundException(input.userId()));
            EstateMember member = new EstateMember();
            member.setEstate(saved);
            member.setUser(user);
            member.setRole(input.role());
            estateMemberRepository.save(member);
        }

        return toDTO(saved);
    }

    /**
     * Updates estate metadata (name, description).
     * Accessible to both PLATFORM_ADMIN (via admin controller) and estate MANAGER
     * (via estate-scoped controller).
     */
    @Transactional
    public EstateDTO updateEstate(UUID id, UpdateEstateRequest req) {
        Estate estate = findOrThrow(id);
        if (estateRepository.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new EstateNameTakenException(req.name());
        }
        estate.setName(req.name());
        estate.setDescription(req.description());
        return toDTO(estateRepository.save(estate));
    }

    @Transactional
    public void deleteEstate(UUID id) {
        findOrThrow(id);
        int buildingCount = (int) buildingRepository.countByEstateId(id);
        if (buildingCount > 0) {
            throw new EstateHasBuildingsException(buildingCount);
        }
        estateRepository.deleteById(id);
    }

    // ─── Phase 6: Alert computation ───────────────────────────────────────────

    private int computeBoilerAlerts(UUID estateId) {
        int warningMonths = platformConfigService.getIntValue(
                estateId,
                EstatePlatformConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS,
                3);
        LocalDate threshold = LocalDate.now().plusMonths(warningMonths);

        return (int) boilerRepository.findActiveByEstateId(estateId)
                .stream()
                .filter(b -> {
                    if (b.getNextServiceDate() == null) return false;
                    return b.getNextServiceDate().isBefore(threshold);
                })
                .count();
    }

    private int computeFireExtinguisherAlerts(UUID estateId) {
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return (int) fireExtinguisherRepository.findByBuildingEstateId(estateId)
                .stream()
                .filter(ext -> {
                    if (ext.getRevisions().isEmpty()) return true;
                    LocalDate latest = ext.getRevisions().get(0).getRevisionDate();
                    return latest.isBefore(oneYearAgo);
                })
                .count();
    }

    private int computeLeaseEndAlerts(UUID estateId) {
        return (int) leaseService.getAlerts(estateId)
                .stream()
                .filter(LeaseAlertDTO::isEndNoticeAlertActive)
                .count();
    }

    private int computeIndexationAlerts(UUID estateId) {
        return (int) leaseService.getAlerts(estateId)
                .stream()
                .filter(LeaseAlertDTO::isIndexationAlertActive)
                .count();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Estate findOrThrow(UUID id) {
        return estateRepository.findById(id)
                .orElseThrow(() -> new EstateNotFoundException(id));
    }

    private EstateDTO toDTO(Estate estate) {
        int memberCount = (int) estateMemberRepository.countByEstateId(estate.getId());
        int buildingCount = (int) buildingRepository.countByEstateId(estate.getId());
        String createdByUsername = estate.getCreatedBy() != null
                ? estate.getCreatedBy().getUsername()
                : null;
        return new EstateDTO(
                estate.getId(),
                estate.getName(),
                estate.getDescription(),
                memberCount,
                buildingCount,
                estate.getCreatedAt(),
                createdByUsername);
    }

    private EstateSummaryDTO toSummaryDTO(Estate estate, EstateRole myRole) {
        int buildingCount = (int) buildingRepository.countByEstateId(estate.getId());
        int unitCount = (int) housingUnitRepository.countByBuilding_Estate_Id(estate.getId());
        return new EstateSummaryDTO(
                estate.getId(),
                estate.getName(),
                estate.getDescription(),
                myRole,
                buildingCount,
                unitCount);
    }

    private EstateMemberDTO toMemberDTO(EstateMember m) {
        return new EstateMemberDTO(
                m.getUser().getId(),
                m.getUser().getUsername(),
                m.getUser().getEmail(),
                m.getRole(),
                m.getAddedAt());
    }
}
