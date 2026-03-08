package com.immocare.model.dto;

/**
 * Per-row enrichment provided by the user during the preview step.
 *
 * <p>Matched against imported rows by {@code fingerprint}.
 * All relation fields are optional — only non-null values are applied.
 *
 * @param fingerprint     SHA-256 fingerprint of the row (must match exactly)
 * @param subcategoryId   subcategory to assign (optional)
 * @param leaseId         lease to link — income only (optional)
 * @param housingUnitId   housing unit to link (optional)
 * @param buildingId      building to link — derived from unit when unitId is set (optional)
 * @param directionOverride  "INCOME" or "EXPENSE" — overrides parser-detected direction (optional)
 */
public record ImportRowEnrichmentDTO(
    String fingerprint,
    Long subcategoryId,
    Long leaseId,
    Long housingUnitId,
    Long buildingId,
    String directionOverride
) {}
