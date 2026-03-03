// core/services/boiler.service.ts — UC011
import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  AddBoilerServiceRecordRequest,
  BoilerDTO,
  BoilerServiceRecordDTO,
  SaveBoilerRequest,
} from "../../models/boiler.model";

@Injectable({ providedIn: "root" })
export class BoilerService {
  constructor(private http: HttpClient) {}

  // ─── Housing Unit ─────────────────────────────────────────────────────────

  getUnitBoilers(unitId: number): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>(
      `/api/v1/housing-units/${unitId}/boilers`,
    );
  }

  createUnitBoiler(
    unitId: number,
    req: SaveBoilerRequest,
  ): Observable<BoilerDTO> {
    return this.http.post<BoilerDTO>(
      `/api/v1/housing-units/${unitId}/boilers`,
      req,
    );
  }

  // ─── Building ─────────────────────────────────────────────────────────────

  getBuildingBoilers(buildingId: number): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>(
      `/api/v1/buildings/${buildingId}/boilers`,
    );
  }

  createBuildingBoiler(
    buildingId: number,
    req: SaveBoilerRequest,
  ): Observable<BoilerDTO> {
    return this.http.post<BoilerDTO>(
      `/api/v1/buildings/${buildingId}/boilers`,
      req,
    );
  }

  // ─── Owner-agnostic (by boiler id) ───────────────────────────────────────

  getById(id: number): Observable<BoilerDTO> {
    return this.http.get<BoilerDTO>(`/api/v1/boilers/${id}`);
  }

  update(id: number, req: SaveBoilerRequest): Observable<BoilerDTO> {
    return this.http.put<BoilerDTO>(`/api/v1/boilers/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/boilers/${id}`);
  }

  /**
   * Returns boilers with service due soon — used by inline boiler cards.
   * The global alerts page uses AlertService.getAlerts() instead.
   */
  getServiceAlerts(): Observable<BoilerDTO[]> {
    return this.http.get<BoilerDTO[]>("/api/v1/boilers/alerts");
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
