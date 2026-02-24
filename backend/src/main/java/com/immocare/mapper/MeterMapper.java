package com.immocare.mapper;

import com.immocare.model.dto.MeterDTO;
import com.immocare.model.entity.Meter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * MapStruct mapper for Meter ↔ MeterDTO.
 * Computes {@code status} field from {@code endDate}.
 */
@Mapper(componentModel = "spring")
public interface MeterMapper {

    @Mapping(target = "status", source = ".", qualifiedByName = "computeStatus")
    MeterDTO toDTO(Meter meter);

    @Named("computeStatus")
    default String computeStatus(Meter meter) {
        return meter.getEndDate() == null ? "ACTIVE" : "CLOSED";
    }
}
