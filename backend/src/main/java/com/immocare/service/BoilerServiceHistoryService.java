package com.immocare.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BoilerNotFoundException;
import com.immocare.model.dto.BoilerServiceDTOs.AddBoilerServiceRecordRequest;
import com.immocare.model.dto.BoilerServiceDTOs.BoilerServiceRecordDTO;
import com.immocare.model.dto.BoilerServiceDTOs.ServiceStatus;
import com.immocare.model.dto.EstateConfigDTOs;
import com.immocare.model.entity.Boiler;
import com.immocare.model.entity.BoilerServiceRecord;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BoilerServiceRecordRepository;
import com.immocare.repository.BuildingRepository;
import com.immocare.repository.HousingUnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for boiler maintenance history — UC012
 * (UC011.005/UC011.006/UC011.007).
 * UC004_ESTATE_PLACEHOLDER Phase 5: valid_until calculation now uses
 * estate-scoped validity rules.
 * Alert threshold is now read from estate-scoped platform config.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoilerServiceHistoryService {

    private final BoilerRepository boilerRepository;
    private final BoilerServiceRecordRepository serviceRecordRepository;
    private final EstateConfigService estateConfigService;
    private final BoilerServiceValidityRuleService validityRuleService;
    private final HousingUnitRepository housingUnitRepository;
    private final BuildingRepository buildingRepository;

    // ─── READ ─────────────────────────────────────────────────────────────────

    /** UC011.006 — full history, newest first. */
    public List<BoilerServiceRecordDTO> getHistory(Long boilerId) {
        Boiler boiler = requireBoiler(boilerId);
        UUID estateId = resolveEstateIdFromBoiler(boiler);
        return serviceRecordRepository
                .findByBoilerIdOrderByServiceDateDesc(boilerId)
                .stream()
                .map(r -> toDTO(r, computeStatus(estateId, r.getValidUntil())))
                .toList();
    }

    /** UC011.007 — status of the latest record (for badge on boiler card). */
    public ServiceStatus getLatestStatus(Long boilerId) {
        Boiler boiler = requireBoiler(boilerId);
        UUID estateId = resolveEstateIdFromBoiler(boiler);
        return serviceRecordRepository
                .findTopByBoilerIdOrderByServiceDateDesc(boilerId)
                .map(r -> computeStatus(estateId, r.getValidUntil()))
                .orElse(ServiceStatus.NO_SERVICE);
    }

    // ─── WRITE ────────────────────────────────────────────────────────────────

    /** UC011.005 — record a new service entry. */
    @Transactional
    public BoilerServiceRecordDTO addRecord(Long boilerId, AddBoilerServiceRecordRequest req) {
        if (req.serviceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Service date cannot be in the future");
        }
        Boiler boiler = requireBoiler(boilerId);
        UUID estateId = resolveEstateIdFromBoiler(boiler);

        // Use estate-scoped validity rule; fall back to 24 months if estate unknown
        LocalDate validUntil = req.validUntil() != null
                ? req.validUntil()
                : (estateId != null
                        ? validityRuleService.calculateValidUntil(estateId, req.serviceDate())
                        : req.serviceDate().plusMonths(24));

        BoilerServiceRecord record = new BoilerServiceRecord();
        record.setBoiler(boiler);
        record.setServiceDate(req.serviceDate());
        record.setValidUntil(validUntil);
        record.setNotes(req.notes());
        BoilerServiceRecord saved = serviceRecordRepository.save(record);

        // Keep flat fields in sync (backward compat with existing alerts)
        boiler.setLastServiceDate(req.serviceDate());
        boiler.setNextServiceDate(validUntil);
        boilerRepository.save(boiler);

        return toDTO(saved, computeStatus(estateId, saved.getValidUntil()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Boiler requireBoiler(Long boilerId) {
        return boilerRepository.findById(boilerId)
                .orElseThrow(() -> new BoilerNotFoundException(boilerId));
    }

    /**
     * Resolves the estate UUID from the boiler's polymorphic owner chain.
     * HOUSING_UNIT: boiler.ownerId → housing_unit → building → estate
     * BUILDING: boiler.ownerId → building → estate
     * Returns null if resolution fails; config lookups then use default values.
     */
    private UUID resolveEstateIdFromBoiler(Boiler boiler) {
        try {
            if ("HOUSING_UNIT".equals(boiler.getOwnerType())) {
                return housingUnitRepository.findById(boiler.getOwnerId())
                        .map(u -> u.getBuilding().getEstate().getId())
                        .orElse(null);
            }
            if ("BUILDING".equals(boiler.getOwnerType())) {
                return buildingRepository.findById(boiler.getOwnerId())
                        .map(b -> b.getEstate().getId())
                        .orElse(null);
            }
        } catch (Exception ignored) {
            // If the owner chain is broken, defaults will apply
        }
        return null;
    }

    /**
     * Computes service record status using the estate-scoped alert threshold.
     * Falls back to 3 months when estate context is unavailable.
     */
    private ServiceStatus computeStatus(UUID estateId, LocalDate validUntil) {
        LocalDate today = LocalDate.now();
        if (validUntil.isBefore(today))
            return ServiceStatus.EXPIRED;

        int warningMonths = (estateId != null)
                ? estateConfigService.getIntValue(
                        estateId,
                        EstateConfigDTOs.KEY_BOILER_ALERT_THRESHOLD_MONTHS,
                        3)
                : 3;

        return validUntil.isBefore(today.plusMonths(warningMonths))
                ? ServiceStatus.EXPIRING_SOON
                : ServiceStatus.VALID;
    }

    private BoilerServiceRecordDTO toDTO(BoilerServiceRecord r, ServiceStatus status) {
        return new BoilerServiceRecordDTO(
                r.getId(), r.getBoiler().getId(),
                r.getServiceDate(), r.getValidUntil(),
                r.getNotes(), status, r.getCreatedAt());
    }
}
