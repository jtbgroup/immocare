import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import {
  BatchCreateRoomsRequest,
  CreateRoomRequest,
  Room,
  RoomListResponse,
  UpdateRoomRequest,
} from "../../models/room.model";
import { ActiveEstateService } from "./active-estate.service";
/**
 * Angular service for Room API calls.
 * UC004 - Manage Rooms.
 */
@Injectable({ providedIn: "root" })
export class RoomService {
  constructor(
    private http: HttpClient,
    private readonly activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id)
      throw new Error(
        "No active estate — cannot call RoomService without an estate context.",
      );
    return id;
  }

  private get api(): string {
    return environment.apiUrl + `/estates/${this.estateId}/housing-units`;
  }

  /** GET /api/v1/estates/{estateId}/housing-units/{unitId}/rooms */
  getRooms(unitId: number): Observable<RoomListResponse> {
    return this.http.get<RoomListResponse>(`${this.api}/${unitId}/rooms`);
  }

  /** POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms */
  createRoom(unitId: number, request: CreateRoomRequest): Observable<Room> {
    return this.http.post<Room>(`${this.api}/${unitId}/rooms`, request);
  }

  /** PUT /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id} */
  updateRoom(
    unitId: number,
    id: number,
    request: UpdateRoomRequest,
  ): Observable<Room> {
    return this.http.put<Room>(`${this.api}/${unitId}/rooms/${id}`, request);
  }

  /** DELETE /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/{id} */
  deleteRoom(unitId: number, id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(
      `${this.api}/${unitId}/rooms/${id}`,
    );
  }

  /** POST /api/v1/estates/{estateId}/housing-units/{unitId}/rooms/batch */
  batchCreateRooms(
    unitId: number,
    request: BatchCreateRoomsRequest,
  ): Observable<Room[]> {
    return this.http.post<Room[]>(`${this.api}/${unitId}/rooms/batch`, request);
  }
}
