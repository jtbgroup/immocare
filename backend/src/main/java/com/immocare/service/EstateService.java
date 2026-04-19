package com.immocare.service;

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
import com.immocare.exception.UserNotFoundException;
import com.immocare.model.dto.EstateDTOs.AddEstateMemberRequest;
import com.immocare.model.dto.EstateDTOs.CreateEstateRequest;
import com.immocare.model.dto.EstateDTOs.EstateDashboardDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberDTO;
import com.immocare.model.dto.EstateDTOs.EstatePendingAlertsDTO;
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstateDTOs.EstateSummaryDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.dto.EstateDTOs.UpdateEstateRequest;
import com.immocare.model.dto.EstatePlatformConfigDTOs;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.LeaseRepository;
import com.immocare.repository.UserRepository;

/**
 * Business logic for UC016 — Manage Estates.
 * UC016 Phase 5: createEstate() now seeds default platform config and boiler
 * service validity rule for every new estate in the same transaction.
 */
@Service
@Transactional
public class EstateService {

    private final EstateRepository estateRepository;
    private final EstateMemberRepository estateMemberRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final LeaseRepository leaseRepository;
    private final PlatformConfigService platformConfigService;
    private final BoilerServiceValidityRuleService validityRuleService;

    public EstateService(EstateRepository estateRepository,
            EstateMemberRepository estateMemberRepository,
            BuildingRepository buildingRepository,
            UserRepository userRepository,
            LeaseRepository leaseRepository,
            PlatformConfigService platformConfigService,
            BoilerServiceValidityRuleService validityRuleService) {
        this.estateRepository = estateRepository;
        this.estateMemberRepository = estateMemberRepository;
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.leaseRepository = leaseRepository;
        this.platformConfigService = platformConfigService;
        this.validityRuleService = validityRuleService;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EstateDTO> getAllEstates(String search, Pageable pageable) {
        Page<Estate> page = (search != null && !search.isBlank())
                ? estateRepository.searchByName(search, pageable)
                : estateRepository.findAllByOrderByNameAsc(pageable);
        return page.map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<EstateSummaryDTO> getMyEstates(Long currentUserId, boolean isPlatformAdmin) {
        if (isPlatformAdmin) {
            return estateRepository.findAllByOrderByNameAsc(Pageable.unpaged())
                    .stream()
                    .map(e -> toSummaryDTO(e, null))
                    .toList();
        }
        return estateMemberRepository.findByUserId(currentUserId)
                .stream()
                .map(m -> toSummaryDTO(m.getEstate(), m.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public EstateDTO getEstateById(UUID id) {
        return toDTO(findEstateOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EstateDashboardDTO getDashboard(UUID estateId) {
        Estate estate = findEstateOrThrow(estateId);

        int totalBuildings = (int) buildingRepository.countByEstateId(estateId);
        int totalUnits = 0; // populated in Phase 6
        int activeLeases = (int) leaseRepository.countByHousingUnit_Building_Estate_IdAndStatus(
                estateId, com.immocare.model.enums.LeaseStatus.ACTIVE);

        EstatePendingAlertsDTO alerts = new EstatePendingAlertsDTO(0, 0, 0, 0);

        return new EstateDashboardDTO(
                estateId,
                estate.getName(),
                totalBuildings,
                totalUnits,
                activeLeases,
                alerts);
    }

    @Transactional(readOnly = true)
    public List<EstateMemberDTO> getMembers(UUID estateId) {
        findEstateOrThrow(estateId);
        return estateMemberRepository.findByEstateIdOrderByUserUsernameAsc(estateId)
                .stream()
                .map(this::toMemberDTO)
                .toList();
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    /**
     * Creates a new estate and seeds all default configuration atomically.
     * UC016 Phase 5: seeds platform config entries and default boiler validity rule.
     */
    public EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId) {
        // BR-UC016-01: name uniqueness (case-insensitive)
        if (estateRepository.existsByNameIgnoreCase(req.name())) {
            throw new EstateNameTakenException(req.name());
        }

        AppUser createdBy = userRepository.findById(createdByUserId).orElse(null);

        Estate estate = new Estate();
        estate.setName(req.name().trim());
        estate.setDescription(req.description());
        estate.setCreatedBy(createdBy);
        estate = estateRepository.save(estate);

        // ── Phase 5: seed default platform config ─────────────────────────────
        platformConfigService.seedDefaultConfig(estate, EstatePlatformConfigDTOs.DEFAULT_CONFIG);

        // ── Phase 5: seed default boiler service validity rule ────────────────
        validityRuleService.seedDefaultRule(estate);

        // ── Optionally assign first manager (US096) ───────────────────────────
        if (req.firstManagerId() != null) {
            AppUser firstManager = userRepository.findById(req.firstManagerId())
                    .orElseThrow(() -> new UserNotFoundException(req.firstManagerId()));
            EstateMember member = new EstateMember();
            member.setEstate(estate);
            member.setUser(firstManager);
            member.setRole(EstateRole.MANAGER);
            estateMemberRepository.save(member);
        }

        return toDTO(estate);
    }

    public EstateDTO updateEstate(UUID id, UpdateEstateRequest req) {
        Estate estate = findEstateOrThrow(id);

        // BR-UC016-01: name uniqueness excluding self
        if (estateRepository.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new EstateNameTakenException(req.name());
        }

        estate.setName(req.name().trim());
        estate.setDescription(req.description());
        return toDTO(estateRepository.save(estate));
    }

    /**
     * Deletes an estate.
     * BR-UC016-09: blocked if the estate contains buildings.
     */
    public void deleteEstate(UUID id) {
        findEstateOrThrow(id);
        int buildingCount = (int) buildingRepository.countByEstateId(id);
        if (buildingCount > 0) {
            throw new EstateHasBuildingsException(buildingCount);
        }
        estateRepository.deleteById(id);
    }

    public EstateMemberDTO addMember(UUID estateId, AddEstateMemberRequest req, Long currentUserId) {
        findEstateOrThrow(estateId);

        AppUser user = userRepository.findById(req.userId())
                .orElseThrow(() -> new UserNotFoundException(req.userId()));

        if (estateMemberRepository.existsByEstateIdAndUserId(estateId, req.userId())) {
            throw new EstateMemberAlreadyExistsException();
        }

        Estate estate = findEstateOrThrow(estateId);
        EstateMember member = new EstateMember();
        member.setEstate(estate);
        member.setUser(user);
        member.setRole(req.role());
        return toMemberDTO(estateMemberRepository.save(member));
    }

    public EstateMemberDTO updateMemberRole(UUID estateId, Long userId,
            UpdateEstateMemberRoleRequest req, Long currentUserId) {
        findEstateOrThrow(estateId);

        // BR-UC016-04: cannot change own role
        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot change your own role");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        // BR-UC016-02: cannot demote last MANAGER
        if (member.getRole() == EstateRole.MANAGER
                && req.role() != EstateRole.MANAGER
                && estateMemberRepository.countByEstateIdAndRole(estateId, "MANAGER") <= 1) {
            throw new EstateLastManagerException();
        }

        member.setRole(req.role());
        return toMemberDTO(estateMemberRepository.save(member));
    }

    public void removeMember(UUID estateId, Long userId, Long currentUserId) {
        findEstateOrThrow(estateId);

        // BR-UC016-03: cannot remove self
        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot remove yourself from an estate");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        // BR-UC016-02: cannot remove last MANAGER
        if (member.getRole() == EstateRole.MANAGER
                && estateMemberRepository.countByEstateIdAndRole(estateId, "MANAGER") <= 1) {
            throw new EstateLastManagerException();
        }

        estateMemberRepository.delete(member);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Estate findEstateOrThrow(UUID id) {
        return estateRepository.findById(id)
                .orElseThrow(() -> new EstateNotFoundException(id));
    }

    private EstateDTO toDTO(Estate estate) {
        int memberCount = (int) estateMemberRepository.countByEstateId(estate.getId());
        int buildingCount = (int) buildingRepository.countByEstateId(estate.getId());
        return new EstateDTO(
                estate.getId(),
                estate.getName(),
                estate.getDescription(),
                memberCount,
                buildingCount,
                estate.getCreatedAt(),
                estate.getCreatedBy() != null ? estate.getCreatedBy().getUsername() : null);
    }

    private EstateSummaryDTO toSummaryDTO(Estate estate, EstateRole myRole) {
        int buildingCount = (int) buildingRepository.countByEstateId(estate.getId());
        return new EstateSummaryDTO(
                estate.getId(),
                estate.getName(),
                estate.getDescription(),
                myRole,
                buildingCount,
                0); // unitCount populated in Phase 6
    }

    private EstateMemberDTO toMemberDTO(EstateMember member) {
        return new EstateMemberDTO(
                member.getUser().getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRole(),
                member.getAddedAt());
    }
}
