package com.immocare.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.immocare.model.dto.BuildingDTO;
import com.immocare.model.dto.CreateBuildingRequest;
import com.immocare.model.dto.UpdateBuildingRequest;
import com.immocare.model.entity.Building;

/**
 * MapStruct mapper for Building entity conversions.
 * UC016 Phase 2: estate field is set manually in BuildingService — never mapped here.
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BuildingMapper {

    /**
     * Convert Building entity to BuildingDTO.
     * ownerName is derived from the owner Person (firstName + lastName).
     * ownerId is mapped from owner.id.
     */
    @Mapping(target = "estateId", source = "estate.id")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", expression = "java(building.getOwner() == null ? null : (building.getOwner().getFirstName() + \" \" + building.getOwner().getLastName()).trim())")
    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "unitCount", expression = "java(0L)")
    BuildingDTO toDTO(Building building);

    /**
     * Convert Building entity to BuildingDTO with unit count.
     */
    @Mapping(target = "estateId", source = "building.estate.id")
    @Mapping(target = "ownerId", source = "building.owner.id")
    @Mapping(target = "ownerName", expression = "java(building.getOwner() == null ? null : (building.getOwner().getFirstName() + \" \" + building.getOwner().getLastName()).trim())")
    @Mapping(target = "createdByUsername", source = "building.createdBy.username")
    @Mapping(target = "unitCount", source = "unitCount")
    BuildingDTO toDTOWithUnitCount(Building building, Long unitCount);

    /**
     * Convert CreateBuildingRequest to Building entity.
     * estate and owner are set manually in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estate", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Building toEntity(CreateBuildingRequest request);

    /**
     * Update existing Building entity from UpdateBuildingRequest.
     * estate and owner are set manually in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estate", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(UpdateBuildingRequest request, @MappingTarget Building building);
}
