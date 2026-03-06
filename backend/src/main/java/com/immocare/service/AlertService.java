package com.immocare.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.immocare.model.dto.AlertDTO;
import com.immocare.model.dto.BoilerDTO;
import com.immocare.model.dto.LeaseAlertDTO;

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

        // ─── Public API ──────────────────────────────────────────────────────────

        /**
         * Returns all pending alerts from all sources, sorted by deadline ASC.
         * Null deadlines are sorted last.
         */
        public List<AlertDTO> getAll() {
                List<AlertDTO> alerts = new ArrayList<>();
                alerts.addAll(leaseAlertsToDTO());
                alerts.addAll(boilerAlertsToDTO());
                alerts.sort(Comparator.comparing(AlertDTO::deadline,
                                Comparator.nullsLast(Comparator.naturalOrder())));
                return alerts;
        }

        /** Returns only the total count — used by the bell badge. */
        public int getCount() {
                return getAll().size();
        }

        // ─── Lease alerts ────────────────────────────────────────────────────────

        private List<AlertDTO> leaseAlertsToDTO() {
                return leaseService.getAlerts().stream()
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

        private List<AlertDTO> boilerAlertsToDTO() {
                return boilerService.getServiceAlerts().stream()
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
}
