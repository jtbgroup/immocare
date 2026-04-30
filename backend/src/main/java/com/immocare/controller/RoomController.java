package com.immocare.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.immocare.model.dto.BatchCreateRoomsRequest;
import com.immocare.model.dto.CreateRoomRequest;
import com.immocare.model.dto.RoomDTO;
import com.immocare.model.dto.RoomListResponse;
import com.immocare.model.dto.UpdateRoomRequest;
import com.immocare.service.RoomService;

import jakarta.validation.Valid;

/**
 * REST controller for Room management.
 * UC004 - Manage Rooms (UC007.001–UC007.005).
 * UC004_ESTATE_PLACEHOLDER: all routes are now scoped to an estate.
 *
 * Endpoints:
 * GET /api/v1/estates/{estateId}/housing-units/{unitId}/rooms
 * POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms
 * PUT /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id}
 * DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id}
 * POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/batch
 */
@RestController
@RequestMapping("/api/v1/estates/{estateId}/housing-units/{unitId}/rooms")
@PreAuthorize("@security.isMemberOf(#estateId)")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  /** UC007.005 - View Room Composition */
  @GetMapping
  public ResponseEntity<RoomListResponse> getRooms(
      @PathVariable UUID estateId,
      @PathVariable Long unitId) {
    return ResponseEntity.ok(roomService.getRoomsByUnit(unitId));
  }

  /** UC007.001 - Add Room */
  @PostMapping
  @PreAuthorize("@security.isManagerOf(#estateId)")
  public ResponseEntity<RoomDTO> createRoom(
      @PathVariable UUID estateId,
      @PathVariable Long unitId,
      @Valid @RequestBody CreateRoomRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.createRoom(unitId, request));
  }

  /** UC007.002 - Edit Room */
  @PutMapping("/{id}")
  @PreAuthorize("@security.isManagerOf(#estateId)")
  public ResponseEntity<RoomDTO> updateRoom(
      @PathVariable UUID estateId,
      @PathVariable Long unitId,
      @PathVariable Long id,
      @Valid @RequestBody UpdateRoomRequest request) {
    return ResponseEntity.ok(roomService.updateRoom(unitId, id, request));
  }

  /** UC007.003 - Delete Room */
  @DeleteMapping("/{id}")
  @PreAuthorize("@security.isManagerOf(#estateId)")
  public ResponseEntity<Map<String, String>> deleteRoom(
      @PathVariable UUID estateId,
      @PathVariable Long unitId,
      @PathVariable Long id) {
    roomService.deleteRoom(unitId, id);
    return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
  }

  /** UC007.004 - Quick Add Multiple Rooms */
  @PostMapping("/batch")
  @PreAuthorize("@security.isManagerOf(#estateId)")
  public ResponseEntity<List<RoomDTO>> batchCreateRooms(
      @PathVariable UUID estateId,
      @PathVariable Long unitId,
      @Valid @RequestBody BatchCreateRoomsRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(roomService.batchCreateRooms(unitId, request));
  }
}