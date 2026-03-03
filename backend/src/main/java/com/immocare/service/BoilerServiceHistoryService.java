package com.immocare.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.immocare.exception.BoilerNotFoundException;
import com.immocare.model.dto.BoilerServiceDTOs.AddBoilerServiceRecordRequest;
import com.immocare.model.dto.BoilerServiceDTOs.BoilerServiceRecordDTO;
import com.immocare.model.dto.BoilerServiceDTOs.ServiceStatus;
import com.immocare.model.dto.PlatformConfigDTOs;
import com.immocare.model.entity.Boiler;
import com.immocare.model.entity.BoilerServiceRecord;
import com.immocare.repository.BoilerRepository;
import com.immocare.repository.BoilerServiceRecordRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for boiler maintenance history — UC011 (US064/US065/US066).
 * Kept separate from BoilerService to avoid the BoilerServiceRecord name
 * collision.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoilerServiceHistoryService {

    private final BoilerRepository boilerRepository;
    private final BoilerServiceRecordRepository serviceRecordRepository;
    private final PlatformConfigService platformConfigService;

    // ─── READ ─────────────────────────────────────────────────────────────────

    /** US065 — full history, newest first. */
    public List<BoilerServiceRecordDTO> getHistory(Long boilerId) {
        requireBoiler(boilerId);
        return serviceRecordRepository
                .findByBoilerIdOrderByServiceDateDesc(boilerId)
                .stream()
                .map(r -> toDTO(r, computeStatus(r.getValidUntil())))
                .toList();
    }

    /** US066 — status of the latest record (for badge on boiler card). */
    public ServiceStatus getLatestStatus(Long boilerId) {
        return serviceRecordRepository
                .findTopByBoilerIdOrderByServiceDateDesc(boilerId)
                .map(r -> computeStatus(r.getValidUntil()))
                .orElse(ServiceStatus.NO_SERVICE);
    }

    // ─── WRITE ────────────────────────────────────────────────────────────────

    /** US064 — record a new service entry. */
    @Transactional
    public BoilerServiceRecordDTO addRecord(Long boilerId,
            AddBoilerServiceRecordRequest req) {
        if (req.serviceDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Service date cannot be in the future");
        }
        Boiler boiler = requireBoiler(boilerId);

        LocalDate validUntil = req.validUntil() != null
                ? req.validUntil()
                : calculateValidUntil(req.serviceDate());

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

        return toDTO(saved, computeStatus(saved.getValidUntil()));
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private Boiler requireBoiler(Long boilerId) {
        return boilerRepository.findById(boilerId)
                .orElseThrow(() -> new BoilerNotFoundException(boilerId));
    }

    /**
     * Default validity = serviceDate + boiler_service_warning_days.
     * Falls back to 365 days if the config key is missing.
     */
    private LocalDate calculateValidUntil(LocalDate serviceDate) {
        int days = platformConfigService.getInt(
                PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 365);
        return serviceDate.plusDays(days);
    }

    private ServiceStatus computeStatus(LocalDate validUntil) {
        LocalDate today = LocalDate.now();
        if (validUntil.isBefore(today))
            return ServiceStatus.EXPIRED;
        int warningDays = platformConfigService.getInt(
                PlatformConfigDTOs.KEY_BOILER_SERVICE_WARNING_DAYS, 30);
        return validUntil.isAfter(today.plusDays(warningDays))
                ? ServiceStatus.VALID
                : ServiceStatus.EXPIRING_SOON;
    }

    private BoilerServiceRecordDTO toDTO(BoilerServiceRecord r, ServiceStatus status) {
        return new BoilerServiceRecordDTO(
                r.getId(), r.getBoiler().getId(),
                r.getServiceDate(), r.getValidUntil(),
                r.getNotes(), status, r.getCreatedAt());
    }
}
