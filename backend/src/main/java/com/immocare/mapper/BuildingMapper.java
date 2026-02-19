package com.immocare.mapper;

import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Building entity conversions.
 * Handles mapping between entities and DTOs.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BuildingMapper {

  /**
   * Convert Building entity to BuildingDTO.
   * 
   * @param building the entity
   * @return the DTO
   */
  @Mapping(target = "createdByUsername", source = "createdBy.username")
  @Mapping(target = "unitCount", expression = "java(0L)")
  BuildingDTO toDTO(Building building);

  /**
   * Convert Building entity to BuildingDTO with unit count.
   * 
   * @param building the entity
   * @param unitCount the number of housing units
   * @return the DTO
   */
  @Mapping(target = "createdByUsername", source = "building.createdBy.username")
  @Mapping(target = "unitCount", source = "unitCount")
  BuildingDTO toDTOWithUnitCount(Building building, Long unitCount);

  /**
   * Convert CreateBuildingRequest to Building entity.
   * 
   * @param request the request DTO
   * @return the entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  Building toEntity(CreateBuildingRequest request);

  /**
   * Update existing Building entity from UpdateBuildingRequest.
   * 
   * @param request the request DTO
   * @param building the entity to update
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateEntityFromRequest(UpdateBuildingRequest request, @MappingTarget Building building);
}
