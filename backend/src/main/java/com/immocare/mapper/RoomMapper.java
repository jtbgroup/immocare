package com.immocare.mapper;

import com.immocare.model.dto.CreateRoomRequest;
import com.immocare.model.dto.RoomDTO;
import com.immocare.model.dto.UpdateRoomRequest;
import com.immocare.model.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for Room entity conversions.
 * UC003 - Manage Rooms.
 */
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RoomMapper {

  /**
   * Convert Room entity to RoomDTO.
   */
  @Mapping(target = "housingUnitId", source = "housingUnit.id")
  RoomDTO toDTO(Room room);

  /**
   * Convert CreateRoomRequest to Room entity.
   * housingUnit is set manually in the service.
   */
  @Mapping(target = "id",          ignore = true)
  @Mapping(target = "housingUnit", ignore = true)
  @Mapping(target = "createdAt",   ignore = true)
  @Mapping(target = "updatedAt",   ignore = true)
  Room toEntity(CreateRoomRequest request);

  /**
   * Apply UpdateRoomRequest fields onto an existing Room entity.
   */
  @Mapping(target = "id",          ignore = true)
  @Mapping(target = "housingUnit", ignore = true)
  @Mapping(target = "createdAt",   ignore = true)
  @Mapping(target = "updatedAt",   ignore = true)
  void updateEntityFromRequest(UpdateRoomRequest request, @MappingTarget Room room);
}
