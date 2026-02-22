package com.immocare.controller;

import com.immocare.model.dto.BatchCreateRoomsRequest;
import com.immocare.model.dto.CreateRoomRequest;
import com.immocare.model.dto.RoomDTO;
import com.immocare.model.dto.RoomListResponse;
import com.immocare.model.dto.UpdateRoomRequest;
import com.immocare.service.RoomService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Room management.
 * Implements UC003 - Manage Rooms (US012–US016).
 *
 * Endpoints:
 *   GET    /api/v1/housing-units/{unitId}/rooms          → list rooms + total (US016)
 *   POST   /api/v1/housing-units/{unitId}/rooms          → create room (US012)
 *   PUT    /api/v1/housing-units/{unitId}/rooms/{id}     → update room (US013)
 *   DELETE /api/v1/housing-units/{unitId}/rooms/{id}     → delete room (US014)
 *   POST   /api/v1/housing-units/{unitId}/rooms/batch    → batch create (US015)
 */
@RestController
@RequestMapping("/api/v1/housing-units/{unitId}/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  /**
   * List all rooms for a housing unit, with total surface.
   * Implements US016 - View Room Composition.
   */
  @GetMapping
  public ResponseEntity<RoomListResponse> getRooms(@PathVariable Long unitId) {
    return ResponseEntity.ok(roomService.getRoomsByUnit(unitId));
  }

  /**
   * Create a single room inside a housing unit.
   * Implements US012 - Add Room to Housing Unit.
   */
  @PostMapping
  public ResponseEntity<RoomDTO> createRoom(
      @PathVariable Long unitId,
      @Valid @RequestBody CreateRoomRequest request) {
    RoomDTO created = roomService.createRoom(unitId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  /**
   * Update an existing room.
   * Implements US013 - Edit Room.
   */
  @PutMapping("/{id}")
  public ResponseEntity<RoomDTO> updateRoom(
      @PathVariable Long unitId,
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoomRequest request) {
    RoomDTO updated = roomService.updateRoom(unitId, id, request);
    return ResponseEntity.ok(updated);
  }

  /**
   * Delete a room.
   * Implements US014 - Delete Room.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Map<String, String>> deleteRoom(
      @PathVariable Long unitId,
      @PathVariable Long id) {
    roomService.deleteRoom(unitId, id);
    return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
  }

  /**
   * Batch-create multiple rooms.
   * Implements US015 - Quick Add Multiple Rooms.
   */
  @PostMapping("/batch")
  public ResponseEntity<List<RoomDTO>> batchCreateRooms(
      @PathVariable Long unitId,
      @Valid @RequestBody BatchCreateRoomsRequest request) {
    List<RoomDTO> created = roomService.batchCreateRooms(unitId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
