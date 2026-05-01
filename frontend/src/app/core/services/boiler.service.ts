// core/services/boiler.service.ts — UC004_ESTATE_PLACEHOLDER Phase 2
// All endpoints scoped to the active estate.
import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import {
  AddBoilerServiceRecordRequest,
  BoilerDTO,
  BoilerServiceRecordDTO,
  SaveBoilerRequest,
} from "../../models/boiler.model";
import { ActiveEstateService } from "./active-estate.service";

@Injectable({ providedIn: "root" })
export class BoilerService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id)
      throw new Error(
        "No active estate — cannot call BoilerService without an estate context.",
      );
    return id;
  }

  private get api(): string {
    return environment.apiUrl + `/estates/${this.estateId}`;
  }

  // ─── Housing Unit ─────────────────────────────────────────────────────────

  getUnitBoilers(unitId: number): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>(
      `${this.api}/housing-units/${unitId}/boilers`,
    );
  }

  createUnitBoiler(
    unitId: number,
    req: SaveBoilerRequest,
  ): Observable<BoilerDTO> {
    return this.http.post<BoilerDTO>(
      `${this.api}/housing-units/${unitId}/boilers`,
      req,
    );
  }

  // ─── Building ─────────────────────────────────────────────────────────────

  getBuildingBoilers(buildingId: number): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>(
      `${this.api}/buildings/${buildingId}/boilers`,
    );
  }

  createBuildingBoiler(
    buildingId: number,
    req: SaveBoilerRequest,
  ): Observable<BoilerDTO> {
    return this.http.post<BoilerDTO>(
      `${this.api}/buildings/${buildingId}/boilers`,
      req,
    );
  }

  // ─── Owner-agnostic (by boiler id) ───────────────────────────────────────

  getById(id: number): Observable<BoilerDTO> {
    return this.http.get<BoilerDTO>(`${this.api}/boilers/${id}`);
  }

  update(id: number, req: SaveBoilerRequest): Observable<BoilerDTO> {
    return this.http.put<BoilerDTO>(`${this.api}/boilers/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/boilers/${id}`);
  }

  /**
   * Returns boilers with service due soon — used by inline boiler cards.
   * The global alerts page uses AlertService.getAlerts() instead.
   */
  getServiceAlerts(): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>(`${this.api}/boilers/alerts`);
  }

  getServiceHistory(boilerId: number): Observable<BoilerServiceRecordDTO[]> {
    return this.http.get<BoilerServiceRecordDTO[]>(
      `/api/v1/boilers/${boilerId}/services`,
    );
  }

  addServiceRecord(
    boilerId: number,
    req: AddBoilerServiceRecordRequest,
  ): Observable<BoilerServiceRecordDTO> {
    return this.http.post<BoilerServiceRecordDTO>(
      `/api/v1/boilers/${boilerId}/services`,
      req,
    );
  }
}
