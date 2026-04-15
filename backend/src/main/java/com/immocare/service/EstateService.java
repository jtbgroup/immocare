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
import com.immocare.model.dto.EstateDTOs.EstateDTO;
import com.immocare.model.dto.EstateDTOs.EstateMemberDTO;
import com.immocare.model.dto.EstateDTOs.EstatePendingAlertsDTO;
import com.immocare.model.dto.EstateDTOs.EstateSummaryDTO;
import com.immocare.model.dto.EstateDTOs.UpdateEstateMemberRoleRequest;
import com.immocare.model.dto.EstateDTOs.UpdateEstateRequest;
import com.immocare.model.entity.AppUser;
import com.immocare.model.entity.Estate;
import com.immocare.model.entity.EstateMember;
import com.immocare.model.enums.EstateRole;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.EstateMemberRepository;
import com.immocare.repository.EstateRepository;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.UserRepository;

/**
 * Business logic for UC016 — Manage Estates.
 * Phase 2: getDashboard() now returns real building and unit counts.
 */
@Service
@Transactional(readOnly = true)
public class EstateService {

    private static final String ROLE_MANAGER = "MANAGER";

    private final EstateRepository estateRepository;
    private final EstateMemberRepository estateMemberRepository;
    private final UserRepository userRepository;
    private final BuildingRepository buildingRepository;
    private final HousingUnitRepository housingUnitRepository;

    public EstateService(EstateRepository estateRepository,
                         EstateMemberRepository estateMemberRepository,
                         UserRepository userRepository,
                         BuildingRepository buildingRepository,
                         HousingUnitRepository housingUnitRepository) {
        this.estateRepository = estateRepository;
        this.estateMemberRepository = estateMemberRepository;
        this.userRepository = userRepository;
        this.buildingRepository = buildingRepository;
        this.housingUnitRepository = housingUnitRepository;
    }

    // ─── US095 — List all estates (PLATFORM_ADMIN only) ──────────────────────

    public Page<EstateDTO> getAllEstates(String search, Pageable pageable) {
        Page<Estate> page = (search != null && !search.isBlank())
                ? estateRepository.searchByName(search.trim(), pageable)
                : estateRepository.findAllByOrderByNameAsc(pageable);
        return page.map(this::toDTO);
    }

    // ─── US103 — View my estates ──────────────────────────────────────────────

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

    // ─── US092 — Get by ID ────────────────────────────────────────────────────

    public EstateDTO getEstateById(UUID id) {
        return toDTO(findEstateOrThrow(id));
    }

    // ─── US102 — Dashboard ────────────────────────────────────────────────────

    /**
     * Returns the estate dashboard.
     * Phase 2: totalBuildings and totalUnits are now populated with real counts.
     * activeLeases and pendingAlerts remain 0 until Phase 3/6.
     */
    public EstateDashboardDTO getDashboard(UUID estateId) {
        Estate estate = findEstateOrThrow(estateId);
        int totalBuildings = (int) buildingRepository.countByEstateId(estateId);
        int totalUnits = (int) housingUnitRepository.countByBuilding_Estate_Id(estateId);
        return new EstateDashboardDTO(
                estate.getId(),
                estate.getName(),
                totalBuildings,
                totalUnits,
                0, // activeLeases — Phase 3
                new EstatePendingAlertsDTO(0, 0, 0, 0)); // pendingAlerts — Phase 6
    }

    // ─── US092 — Create estate ────────────────────────────────────────────────

    @Transactional
    public EstateDTO createEstate(CreateEstateRequest req, Long createdByUserId) {
        // BR-UC016-01: name unique case-insensitively
        if (estateRepository.existsByNameIgnoreCase(req.name())) {
            throw new EstateNameTakenException(req.name());
        }

        AppUser createdBy = userRepository.findById(createdByUserId).orElse(null);

        Estate estate = new Estate();
        estate.setName(req.name().trim());
        estate.setDescription(req.description());
        estate.setCreatedBy(createdBy);
        estate = estateRepository.save(estate);

        // US096: assign first manager if provided
        if (req.firstManagerId() != null) {
            AppUser manager = userRepository.findById(req.firstManagerId())
                    .orElseThrow(() -> new UserNotFoundException(req.firstManagerId()));
            EstateMember member = new EstateMember();
            member.setEstate(estate);
            member.setUser(manager);
            member.setRole(EstateRole.MANAGER);
            estateMemberRepository.save(member);
        }

        return toDTO(estateRepository.findById(estate.getId()).orElseThrow());
    }

    // ─── US093 — Update estate ────────────────────────────────────────────────

    @Transactional
    public EstateDTO updateEstate(UUID id, UpdateEstateRequest req) {
        Estate estate = findEstateOrThrow(id);

        if (!estate.getName().equalsIgnoreCase(req.name())
                && estateRepository.existsByNameIgnoreCaseAndIdNot(req.name(), id)) {
            throw new EstateNameTakenException(req.name());
        }

        estate.setName(req.name().trim());
        estate.setDescription(req.description());
        return toDTO(estateRepository.save(estate));
    }

    // ─── US094 — Delete estate ────────────────────────────────────────────────

    @Transactional
    public void deleteEstate(UUID id) {
        findEstateOrThrow(id);

        // BR-UC016-09: cannot delete estate with buildings (Phase 2: real count)
        int buildingCount = (int) buildingRepository.countByEstateId(id);
        if (buildingCount > 0) {
            throw new EstateHasBuildingsException(buildingCount);
        }

        estateRepository.deleteById(id);
    }

    // ─── US097 — Get members ──────────────────────────────────────────────────

    public List<EstateMemberDTO> getMembers(UUID estateId) {
        findEstateOrThrow(estateId);
        return estateMemberRepository.findByEstateIdOrderByUserUsernameAsc(estateId)
                .stream()
                .map(this::toMemberDTO)
                .toList();
    }

    // ─── US098 — Add member ───────────────────────────────────────────────────

    @Transactional
    public EstateMemberDTO addMember(UUID estateId, AddEstateMemberRequest req, Long currentUserId) {
        findEstateOrThrow(estateId);

        AppUser user = userRepository.findById(req.userId())
                .orElseThrow(() -> new UserNotFoundException(req.userId()));

        if (estateMemberRepository.existsByEstateIdAndUserId(estateId, req.userId())) {
            throw new EstateMemberAlreadyExistsException();
        }

        Estate estate = estateRepository.getReferenceById(estateId);
        EstateMember member = new EstateMember();
        member.setEstate(estate);
        member.setUser(user);
        member.setRole(req.role());

        return toMemberDTO(estateMemberRepository.save(member));
    }

    // ─── US099 — Update member role ───────────────────────────────────────────

    @Transactional
    public EstateMemberDTO updateMemberRole(UUID estateId,
                                            Long userId,
                                            UpdateEstateMemberRoleRequest req,
                                            Long currentUserId) {
        findEstateOrThrow(estateId);

        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot change your own role");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        if (member.getRole() == EstateRole.MANAGER
                && req.role() == EstateRole.VIEWER
                && estateMemberRepository.countByEstateIdAndRole(estateId, ROLE_MANAGER) <= 1) {
            throw new EstateLastManagerException();
        }

        member.setRole(req.role());
        return toMemberDTO(estateMemberRepository.save(member));
    }

    // ─── US100 — Remove member ────────────────────────────────────────────────

    @Transactional
    public void removeMember(UUID estateId, Long userId, Long currentUserId) {
        findEstateOrThrow(estateId);

        if (userId.equals(currentUserId)) {
            throw new EstateSelfOperationException("You cannot remove yourself from an estate");
        }

        EstateMember member = estateMemberRepository.findByEstateIdAndUserId(estateId, userId)
                .orElseThrow(EstateMemberNotFoundException::new);

        if (member.getRole() == EstateRole.MANAGER
                && estateMemberRepository.countByEstateIdAndRole(estateId, ROLE_MANAGER) <= 1) {
            throw new EstateLastManagerException();
        }

        estateMemberRepository.delete(member);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

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
        int unitCount = (int) housingUnitRepository.countByBuilding_Estate_Id(estate.getId());
        return new EstateSummaryDTO(
                estate.getId(),
                estate.getName(),
                estate.getDescription(),
                myRole,
                buildingCount,
                unitCount);
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
