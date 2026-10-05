import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment.prod";
import {
  AddRevisionRequest,
  EquipmentOwnerType,
  FireExtinguisher,
  SaveFireExtinguisherRequest,
} from "../../models/fire-extinguisher.model";
import { ActiveEstateService } from "../services/active-estate.service";

/**
 * HTTP service for Fire Extinguisher API calls.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all endpoints scoped to the active estate.
 *
 * Routes:
 *   GET/POST  /api/v1/estates/{estateId}/buildings/{buildingId}/fire-extinguishers
 *   GET/PUT/DELETE /api/v1/estates/{estateId}/fire-extinguishers/{id}
 *   POST      /api/v1/estates/{estateId}/fire-extinguishers/{id}/revisions
 *   DELETE    /api/v1/estates/{estateId}/fire-extinguishers/{extId}/revisions/{revId}
 */
@Injectable({ providedIn: "root" })
export class FireExtinguisherService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id)
      throw new Error(
        "No active estate — cannot call FireExtinguisherService without an estate context.",
      );
    return id;
  }

  private get baseExt(): string {
    return environment.apiUrl + `/estates/${this.estateId}/fire-extinguishers`;
  }

  private baseByBuilding(buildingId: number): string {
    return (
      environment.apiUrl +
      `/estates/${this.estateId}/buildings/${buildingId}/fire-extinguishers`
    );
  }

  private baseByUnit(unitId: number): string {
    return (
      environment.apiUrl +
      `/estates/${this.estateId}/housing-units/${unitId}/fire-extinguishers`
    );
  }

  getByOwner(
    ownerType: EquipmentOwnerType,
    ownerId: number,
  ): Observable<FireExtinguisher[]> {
    return this.http.get<FireExtinguisher[]>(
      ownerType === "BUILDING"
        ? this.baseByBuilding(ownerId)
        : this.baseByUnit(ownerId),
    );
  }

  getByBuilding(buildingId: number): Observable<FireExtinguisher[]> {
    return this.getByOwner("BUILDING", buildingId);
  }

  getById(id: number): Observable<FireExtinguisher> {
    return this.http.get<FireExtinguisher>(`${this.baseExt}/${id}`);
  }

  create(
    ownerType: EquipmentOwnerType,
    ownerId: number,
    req: SaveFireExtinguisherRequest,
  ): Observable<FireExtinguisher> {
    return this.http.post<FireExtinguisher>(
      ownerType === "BUILDING"
        ? this.baseByBuilding(ownerId)
        : this.baseByUnit(ownerId),
      req,
    );
  }

  update(
    id: number,
    req: SaveFireExtinguisherRequest,
  ): Observable<FireExtinguisher> {
    return this.http.put<FireExtinguisher>(`${this.baseExt}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseExt}/${id}`);
  }

  addRevision(
    extId: number,
    req: AddRevisionRequest,
  ): Observable<FireExtinguisher> {
    return this.http.post<FireExtinguisher>(
      `${this.baseExt}/${extId}/revisions`,
      req,
    );
  }

  deleteRevision(extId: number, revId: number): Observable<void> {
    return this.http.delete<void>(
      `${this.baseExt}/${extId}/revisions/${revId}`,
    );
  }
}
