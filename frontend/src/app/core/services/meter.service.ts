import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  AddMeterRequest,
  MeterDTO,
  RemoveMeterRequest,
  ReplaceMeterRequest,
} from "../../models/meter.model";

/**
 * HTTP service for UC008 - Manage Meters.
 *
 * Mirrors two symmetric sets of endpoints:
 *   /api/v1/housing-units/{unitId}/meters
 *   /api/v1/buildings/{buildingId}/meters
 */
@Injectable({ providedIn: "root" })
export class MeterService {
  private readonly api = "/api/v1";

  constructor(private readonly http: HttpClient) {}

  // ─── Housing Unit endpoints ───────────────────────────────────────────────

  getUnitMeterHistory(unitId: number): Observable<MeterDTO[]> {
    return this.http.get<MeterDTO[]>(
      `${this.api}/housing-units/${unitId}/meters`,
    );
  }

  getUnitActiveMeters(unitId: number): Observable<MeterDTO[]> {
    const params = new HttpParams().set("status", "active");
    return this.http.get<MeterDTO[]>(
      `${this.api}/housing-units/${unitId}/meters`,
      { params },
    );
  }

  addUnitMeter(unitId: number, req: AddMeterRequest): Observable<MeterDTO> {
    return this.http.post<MeterDTO>(
      `${this.api}/housing-units/${unitId}/meters`,
      req,
    );
  }

  replaceUnitMeter(
    unitId: number,
    meterId: number,
    req: ReplaceMeterRequest,
  ): Observable<MeterDTO> {
    return this.http.put<MeterDTO>(
      `${this.api}/housing-units/${unitId}/meters/${meterId}/replace`,
      req,
    );
  }

  removeUnitMeter(
    unitId: number,
    meterId: number,
    req: RemoveMeterRequest,
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/housing-units/${unitId}/meters/${meterId}`,
      { body: req },
    );
  }

  // ─── Building endpoints ───────────────────────────────────────────────────

  getBuildingMeterHistory(buildingId: number): Observable<MeterDTO[]> {
    return this.http.get<MeterDTO[]>(
      `${this.api}/buildings/${buildingId}/meters`,
    );
  }

  getBuildingActiveMeters(buildingId: number): Observable<MeterDTO[]> {
    const params = new HttpParams().set("status", "active");
    return this.http.get<MeterDTO[]>(
      `${this.api}/buildings/${buildingId}/meters`,
      { params },
    );
  }

  addBuildingMeter(
    buildingId: number,
    req: AddMeterRequest,
  ): Observable<MeterDTO> {
    return this.http.post<MeterDTO>(
      `${this.api}/buildings/${buildingId}/meters`,
      req,
    );
  }

  replaceBuildingMeter(
    buildingId: number,
    meterId: number,
    req: ReplaceMeterRequest,
  ): Observable<MeterDTO> {
    return this.http.put<MeterDTO>(
      `${this.api}/buildings/${buildingId}/meters/${meterId}/replace`,
      req,
    );
  }

  removeBuildingMeter(
    buildingId: number,
    meterId: number,
    req: RemoveMeterRequest,
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.api}/buildings/${buildingId}/meters/${meterId}`,
      { body: req },
    );
  }
}
