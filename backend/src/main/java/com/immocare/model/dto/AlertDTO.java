package com.immocare.model.dto;

import java.time.LocalDate;

/**
 * Unified alert DTO returned by GET /api/v1/alerts.
 *
 * <p>Covers all alert categories (lease, boiler, …) via {@code category}.
 * Frontend uses {@code actionUrl} to navigate to the relevant detail page.
 */
public record AlertDTO(

        /** Broad category: LEASE, BOILER */
        String category,

        /** Sub-type within category: INDEXATION, END_NOTICE, SERVICE_OVERDUE, SERVICE_DUE */
        String type,

        /** Visual severity: WARNING, DANGER */
        String severity,

        /** Human-readable one-liner shown in the alerts table */
        String label,

        /** Deadline or due date — used for sorting */
        LocalDate deadline,

        /** Frontend route to navigate on "View" click (e.g. /leases/12 or /units/3) */
        String actionUrl,

        /** Optional extra context (tenant names, boiler brand, …) */
        String detail
) {}
