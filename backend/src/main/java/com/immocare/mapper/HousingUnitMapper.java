package com.immocare.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.immocare.model.dto.CreateHousingUnitRequest;
import com.immocare.model.dto.HousingUnitDTO;
import com.immocare.model.dto.UpdateHousingUnitRequest;
import com.immocare.model.entity.HousingUnit;

/**
 * MapStruct mapper for HousingUnit entity conversions.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface HousingUnitMapper {

  /**
   * Convert HousingUnit entity to HousingUnitDTO.
   * ownerName is derived from the owner Person (firstName + lastName).
   * effectiveOwnerName and roomCount are set manually in the service.
   */
  @Mapping(target = "buildingId", source = "building.id")
  @Mapping(target = "buildingName", source = "building.name")
  @Mapping(target = "ownerName", expression = "java(unit.getOwner() == null ? null : (unit.getOwner().getFirstName() + \" \" + unit.getOwner().getLastName()).trim())")
  @Mapping(target = "effectiveOwnerName", ignore = true)
  @Mapping(target = "roomCount", ignore = true)
  @Mapping(target = "currentMonthlyRent", ignore = true)
  @Mapping(target = "currentPebScore", ignore = true)
  HousingUnitDTO toDTO(HousingUnit unit);

  /**
   * Convert CreateHousingUnitRequest to HousingUnit entity.
   * building, owner and createdBy are set manually in the service.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "building", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  HousingUnit toEntity(CreateHousingUnitRequest request);

  /**
   * Apply UpdateHousingUnitRequest fields onto an existing HousingUnit entity.
   * building, owner and audit fields are never changed by the mapper.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "building", ignore = true)
  @Mapping(target = "owner", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(UpdateHousingUnitRequest request, @MappingTarget HousingUnit unit);
}