package com.immocare.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.immocare.model.dto.AlertDTO;
import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.EstateConfigDTOs;
import com.immocare.model.dto.LeaseAlertDTO;
import com.immocare.model.entity.PebScoreHistory;
import com.immocare.repository.PebScoreRepository;

import lombok.RequiredArgsConstructor;

/**
 * Cross-cutting service that aggregates all application alerts into a unified
 * list.
 *
 * <p>
 * Add new alert sources here as the application grows (PEB expiry, insurance,
 * …).
 * Each source maps its own DTO to the common {@link AlertDTO}.
 */
@Service
@RequiredArgsConstructor
public class AlertService {

        private final LeaseService leaseService;
        private final BoilerService boilerService;
        private final PebScoreRepository pebScoreRepository;
        private final EstateConfigService estateConfigService;

        // ─── Public API ──────────────────────────────────────────────────────────

        /**
         * Returns all pending alerts from all sources, sorted by deadline ASC.
         * Null deadlines are sorted last.
         */
        public List<AlertDTO> getAll(UUID estateId) {
                List<AlertDTO> alerts = new ArrayList<>();
                alerts.addAll(leaseAlertsToDTO(estateId));
                alerts.addAll(boilerAlertsToDTO(estateId));
                alerts.addAll(pebAlertsToDTO(estateId));
                alerts.sort(Comparator.comparing(AlertDTO::deadline,
                                Comparator.nullsLast(Comparator.naturalOrder())));
                return alerts;
        }

        /** Returns only the total count — used by the bell badge. */
        public int getCount(UUID estateId) {
                return getAll(estateId).size();
        }

        // ─── Lease alerts ────────────────────────────────────────────────────────

        private List<AlertDTO> leaseAlertsToDTO(UUID estateId) {
                return leaseService.getAlerts(estateId).stream()
                                .map(this::fromLease)
                                .toList();
        }

        private AlertDTO fromLease(LeaseAlertDTO src) {
                boolean isEndNotice = "END_NOTICE".equals(src.getAlertType());
                return new AlertDTO(
                                "LEASE",
                                src.getAlertType(),
                                isEndNotice ? "DANGER" : "WARNING",
                                isEndNotice
                                                ? "End notice — " + src.getBuildingName() + " / unit "
                                                                + src.getHousingUnitNumber()
                                                : "Indexation due — " + src.getBuildingName() + " / unit "
                                                                + src.getHousingUnitNumber(),
                                src.getDeadline(),
                                "/leases/" + src.getLeaseId(),
                                src.getTenantNames() != null ? String.join(", ", src.getTenantNames()) : null);
        }

        // ─── Boiler alerts ───────────────────────────────────────────────────────

        private List<AlertDTO> boilerAlertsToDTO(UUID estateId) {
                return boilerService.getServiceAlerts(estateId).stream()
                                .map(this::fromBoiler)
                                .toList();
        }

        private AlertDTO fromBoiler(BoilerDTO src) {
                boolean overdue = src.daysUntilNextService() != null && src.daysUntilNextService() < 0;
                String ownerLabel = "HOUSING_UNIT".equals(src.ownerType()) ? "Unit" : "Building";
                String brandLabel = src.brand() != null ? src.brand() : "Boiler";

                return new AlertDTO(
                                "BOILER",
                                overdue ? "SERVICE_OVERDUE" : "SERVICE_DUE",
                                overdue ? "DANGER" : "WARNING",
                                (overdue ? "Service overdue — " : "Service due — ")
                                                + brandLabel + " (" + ownerLabel + " #" + src.ownerId() + ")",
                                src.nextServiceDate(),
                                src.ownerType().equals("HOUSING_UNIT")
                                                ? "/units/" + src.ownerId()
                                                : "/buildings/" + src.ownerId(),
                                src.daysUntilNextService() != null
                                                ? (overdue
                                                                ? "Overdue by " + Math.abs(src.daysUntilNextService())
                                                                                + " day(s)"
                                                                : "Due in " + src.daysUntilNextService() + " day(s)")
                                                : null);
        }

        // ─── PEB alerts ───────────────────────────────────────────────────────
        private List<AlertDTO> pebAlertsToDTO(UUID estateId) {
                int warningMonths = estateConfigService.getIntValue(
                                estateId,
                                EstateConfigDTOs.KEY_ALERT_PEB_EXPIRY_WARNING_MONTHS,
                                3);
                LocalDate threshold = LocalDate.now().plusMonths(warningMonths);

                return pebScoreRepository.findCurrentScoresWithValidUntilByEstateId(estateId)
                                .stream()
                                .filter(p -> !p.getValidUntil().isAfter(threshold))
                                .map(p -> fromPeb(p))
                                .toList();
        }

        private AlertDTO fromPeb(PebScoreHistory p) {
                boolean expired = p.getValidUntil().isBefore(LocalDate.now());
                Long unitId = p.getHousingUnit().getId();
                String unitNumber = p.getHousingUnit().getUnitNumber();
                String buildingName = p.getHousingUnit().getBuilding().getName();

                return new AlertDTO(
                                "PEB",
                                expired ? "CERTIFICATE_EXPIRED" : "CERTIFICATE_EXPIRING",
                                expired ? "DANGER" : "WARNING",
                                (expired ? "PEB expired — " : "PEB expiring — ")
                                                + buildingName + " / unit " + unitNumber,
                                p.getValidUntil(),
                                "/units/" + unitId,
                                "Certificate valid until " + p.getValidUntil());
        }
}
