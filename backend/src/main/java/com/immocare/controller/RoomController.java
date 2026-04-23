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
 * Implements UC004 - Manage Rooms (UC007.001–UC007.005).
 *
 * Endpoints:
 *   GET    /api/v1/housing-units/{unitId}/rooms          → list rooms + total (UC007.005)
 *   POST   /api/v1/housing-units/{unitId}/rooms          → create room (UC007.001)
 *   PUT    /api/v1/housing-units/{unitId}/rooms/{id}     → update room (UC007.002)
 *   DELETE /api/v1/housing-units/{unitId}/rooms/{id}     → delete room (UC007.003)
 *   POST   /api/v1/housing-units/{unitId}/rooms/batch    → batch create (UC007.004)
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
   * Implements UC007.005 - View Room Composition.
   */
  @GetMapping
  public ResponseEntity<RoomListResponse> getRooms(@PathVariable Long unitId) {
    return ResponseEntity.ok(roomService.getRoomsByUnit(unitId));
  }

  /**
   * Create a single room inside a housing unit.
   * Implements UC007.001 - Add Room to Housing Unit.
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
   * Implements UC007.002 - Edit Room.
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
   * Implements UC007.003 - Delete Room.
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
   * Implements UC007.004 - Quick Add Multiple Rooms.
   */
  @PostMapping("/batch")
  public ResponseEntity<List<RoomDTO>> batchCreateRooms(
      @PathVariable Long unitId,
      @Valid @RequestBody BatchCreateRoomsRequest request) {
    List<RoomDTO> created = roomService.batchCreateRooms(unitId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }
}
