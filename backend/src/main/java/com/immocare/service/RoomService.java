package com.immocare.service;

import com.immocare.exception.HousingUnitNotFoundException;
import com.immocare.exception.RoomNotFoundException;
import com.immocare.mapper.RoomMapper;
import com.immocare.model.dto.BatchCreateRoomsRequest;
import com.immocare.model.dto.CreateRoomRequest;
import com.immocare.model.dto.RoomDTO;
import com.immocare.model.dto.RoomListResponse;
import com.immocare.model.dto.UpdateRoomRequest;
import com.immocare.model.entity.HousingUnit;
import com.immocare.model.entity.Room;
import com.immocare.repository.HousingUnitRepository;
import com.immocare.repository.RoomRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service layer for Room management.
 * Implements business logic for UC003 - Manage Rooms (US012–US016).
 */
@Service
@Transactional(readOnly = true)
public class RoomService {

  private final RoomRepository roomRepository;
  private final HousingUnitRepository housingUnitRepository;
  private final RoomMapper roomMapper;

  public RoomService(RoomRepository roomRepository,
                     HousingUnitRepository housingUnitRepository,
                     RoomMapper roomMapper) {
    this.roomRepository = roomRepository;
    this.housingUnitRepository = housingUnitRepository;
    this.roomMapper = roomMapper;
  }

  // ─── Queries ────────────────────────────────────────────────────────────────

  /**
   * Get all rooms for a housing unit, with computed total surface.
   * Implements US016 - View Room Composition.
   *
   * @param unitId the housing unit ID
   * @return list of rooms + total surface
   */
  public RoomListResponse getRoomsByUnit(Long unitId) {
    assertUnitExists(unitId);
    List<RoomDTO> rooms = roomRepository
        .findByHousingUnitIdOrderByRoomTypeAsc(unitId)
        .stream()
        .map(roomMapper::toDTO)
        .collect(Collectors.toList());
    BigDecimal totalSurface = roomRepository.sumApproximateSurfaceByHousingUnitId(unitId);
    return new RoomListResponse(rooms, totalSurface);
  }

  // ─── Commands ───────────────────────────────────────────────────────────────

  /**
   * Create a single room inside a housing unit.
   * Implements US012 - Add Room to Housing Unit.
   *
   * @param unitId  the housing unit ID
   * @param request the room creation data
   * @return the created room DTO
   */
  @Transactional
  public RoomDTO createRoom(Long unitId, CreateRoomRequest request) {
    HousingUnit unit = findUnit(unitId);
    Room room = roomMapper.toEntity(request);
    room.setHousingUnit(unit);
    Room saved = roomRepository.save(room);
    return roomMapper.toDTO(saved);
  }

  /**
   * Update an existing room.
   * Implements US013 - Edit Room.
   *
   * @param unitId  the housing unit ID (for security / ownership check)
   * @param roomId  the room ID
   * @param request the update data
   * @return the updated room DTO
   */
  @Transactional
  public RoomDTO updateRoom(Long unitId, Long roomId, UpdateRoomRequest request) {
    assertUnitExists(unitId);
    Room room = findRoom(roomId);
    assertRoomBelongsToUnit(room, unitId);
    roomMapper.updateEntityFromRequest(request, room);
    Room updated = roomRepository.save(room);
    return roomMapper.toDTO(updated);
  }

  /**
   * Delete a room from a housing unit.
   * Implements US014 - Delete Room.
   *
   * @param unitId the housing unit ID (ownership check)
   * @param roomId the room ID
   */
  @Transactional
  public void deleteRoom(Long unitId, Long roomId) {
    assertUnitExists(unitId);
    Room room = findRoom(roomId);
    assertRoomBelongsToUnit(room, unitId);
    roomRepository.delete(room);
  }

  /**
   * Batch-create multiple rooms inside a housing unit.
   * Implements US015 - Quick Add Multiple Rooms.
   *
   * @param unitId  the housing unit ID
   * @param request list of room entries (max 20)
   * @return list of created room DTOs
   */
  @Transactional
  public List<RoomDTO> batchCreateRooms(Long unitId, BatchCreateRoomsRequest request) {
    HousingUnit unit = findUnit(unitId);
    List<RoomDTO> created = new ArrayList<>();
    for (BatchCreateRoomsRequest.RoomEntry entry : request.getRooms()) {
      Room room = new Room();
      room.setHousingUnit(unit);
      room.setRoomType(entry.getRoomType());
      room.setApproximateSurface(entry.getApproximateSurface());
      Room saved = roomRepository.save(room);
      created.add(roomMapper.toDTO(saved));
    }
    return created;
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  private HousingUnit findUnit(Long unitId) {
    return housingUnitRepository.findById(unitId)
        .orElseThrow(() -> new HousingUnitNotFoundException(unitId));
  }

  private void assertUnitExists(Long unitId) {
    if (!housingUnitRepository.existsById(unitId)) {
      throw new HousingUnitNotFoundException(unitId);
    }
  }

  private Room findRoom(Long roomId) {
    return roomRepository.findById(roomId)
        .orElseThrow(() -> new RoomNotFoundException(roomId));
  }

  private void assertRoomBelongsToUnit(Room room, Long unitId) {
    if (!room.getHousingUnit().getId().equals(unitId)) {
      throw new RoomNotFoundException(room.getId());
    }
  }
}
