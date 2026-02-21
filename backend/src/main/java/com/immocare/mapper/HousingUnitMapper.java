package com.immocare.mapper;

import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.HousingUnit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for HousingUnit entity conversions.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface HousingUnitMapper {

  /**
   * Convert HousingUnit entity to HousingUnitDTO.
   * effectiveOwnerName and roomCount are set manually in the service.
   */
  @Mapping(target = "buildingId",   source = "building.id")
  @Mapping(target = "buildingName", source = "building.name")
  @Mapping(target = "effectiveOwnerName", ignore = true)
  @Mapping(target = "roomCount",    ignore = true)
  HousingUnitDTO toDTO(HousingUnit unit);

  /**
   * Convert CreateHousingUnitRequest to HousingUnit entity.
   * building is set manually in the service after loading it by buildingId.
   */
  @Mapping(target = "id",        ignore = true)
  @Mapping(target = "building",  ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  HousingUnit toEntity(CreateHousingUnitRequest request);

  /**
   * Apply UpdateHousingUnitRequest fields onto an existing HousingUnit entity.
   * building and audit fields are never changed on update.
   */
  @Mapping(target = "id",        ignore = true)
  @Mapping(target = "building",  ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(UpdateHousingUnitRequest request, @MappingTarget HousingUnit unit);
}
