package com.immocare.mapper;

import com.immocare.model.dto.WaterMeterHistoryDTO;
import com.immocare.model.entity.WaterMeterHistory;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for WaterMeterHistory → WaterMeterHistoryDTO.
 *
 * <p>Uses a plain Spring component because the DTO is a Java record and derived fields
 * (isActive, durationMonths, status) require runtime computation.
 *
 * UC006 - US026, US027, US028, US029, US030.
 */
@Component
public class WaterMeterHistoryMapper {

    /**
     * Maps a {@link WaterMeterHistory} entity to its DTO representation.
     *
     * <p>Computed fields:
     * <ul>
     *   <li>{@code isActive}      — removal_date IS NULL.</li>
     *   <li>{@code durationMonths} — months from installationDate to removalDate (or today).</li>
     *   <li>{@code status}        — "Active" or "Replaced".</li>
     * </ul>
     */
    public WaterMeterHistoryDTO toDTO(WaterMeterHistory entity) {
        boolean active = entity.getRemovalDate() == null;
        LocalDate end = active ? LocalDate.now() : entity.getRemovalDate();
        long durationMonths = ChronoUnit.MONTHS.between(entity.getInstallationDate(), end);

        return new WaterMeterHistoryDTO(
                entity.getId(),
                entity.getHousingUnit().getId(),
                entity.getMeterNumber(),
                entity.getMeterLocation(),
                entity.getInstallationDate(),
                entity.getRemovalDate(),
                entity.getCreatedAt(),
                active,
                durationMonths,
                active ? "Active" : "Replaced"
        );
    }
}
