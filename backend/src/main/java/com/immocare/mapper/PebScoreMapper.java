package com.immocare.mapper;

import com.immocare.model.dto.PebScoreDTO;
import com.immocare.model.entity.PebScoreHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for PebScoreHistory entity conversions.
 * Computed fields (status, expiryWarning) are set in the service layer, not here.
 * UC004 - Manage PEB Scores.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface PebScoreMapper {

    @Mapping(target = "housingUnitId", source = "housingUnit.id")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "expiryWarning", ignore = true)
    PebScoreDTO toDTO(PebScoreHistory entity);
}
