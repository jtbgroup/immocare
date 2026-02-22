package com.immocare.mapper;

import com.immocare.model.dto.RentHistoryDTO;
import com.immocare.model.entity.RentHistory;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for {@link RentHistory} → {@link RentHistoryDTO}.
 * Uses a default method to compute {@code isCurrent} and {@code durationMonths}.
 */
@Mapper(componentModel = "spring")
public interface RentHistoryMapper {

    default RentHistoryDTO toDTO(RentHistory entity) {
        if (entity == null) return null;

        boolean current = entity.getEffectiveTo() == null;
        LocalDate end = current ? LocalDate.now() : entity.getEffectiveTo();
        long months = ChronoUnit.MONTHS.between(entity.getEffectiveFrom(), end);

        return new RentHistoryDTO(
                entity.getId(),
                entity.getHousingUnit().getId(),
                entity.getMonthlyRent(),
                entity.getEffectiveFrom(),
                entity.getEffectiveTo(),
                entity.getNotes(),
                entity.getCreatedAt(),
                current,
                months
        );
    }
}
