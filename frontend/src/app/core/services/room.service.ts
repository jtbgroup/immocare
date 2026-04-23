import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BatchCreateRoomsRequest,
  CreateRoomRequest,
  Room,
  RoomListResponse,
  UpdateRoomRequest,
} from '../../models/room.model';

/**
 * Angular service for Room API calls.
 * UC004 - Manage Rooms.
 */
@Injectable({ providedIn: 'root' })
export class RoomService {
  private base = `${environment.apiUrl}/housing-units`;

  constructor(private http: HttpClient) {}

  /**
   * GET /api/v1/housing-units/{unitId}/rooms
   * UC007.005 - View Room Composition
   */
  getRooms(unitId: number): Observable<RoomListResponse> {
    return this.http.get<RoomListResponse>(`${this.base}/${unitId}/rooms`);
  }

  /**
   * POST /api/v1/housing-units/{unitId}/rooms
   * UC007.001 - Add Room
   */
  createRoom(unitId: number, request: CreateRoomRequest): Observable<Room> {
    return this.http.post<Room>(`${this.base}/${unitId}/rooms`, request);
  }

  /**
   * PUT /api/v1/housing-units/{unitId}/rooms/{id}
   * UC007.002 - Edit Room
   */
  updateRoom(unitId: number, id: number, request: UpdateRoomRequest): Observable<Room> {
    return this.http.put<Room>(`${this.base}/${unitId}/rooms/${id}`, request);
  }

  /**
   * DELETE /api/v1/housing-units/{unitId}/rooms/{id}
   * UC007.003 - Delete Room
   */
  deleteRoom(unitId: number, id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.base}/${unitId}/rooms/${id}`);
  }

  /**
   * POST /api/v1/housing-units/{unitId}/rooms/batch
   * UC007.004 - Quick Add Multiple Rooms
   */
  batchCreateRooms(unitId: number, request: BatchCreateRoomsRequest): Observable<Room[]> {
    return this.http.post<Room[]>(`${this.base}/${unitId}/rooms/batch`, request);
  }
}
