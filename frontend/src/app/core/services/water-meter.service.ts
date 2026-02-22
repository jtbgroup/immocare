import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssignMeterRequest,
  RemoveMeterRequest,
  ReplaceMeterRequest,
  WaterMeterHistory,
} from '../../models/water-meter.model';

/**
 * Angular HTTP service for UC006 — Water Meter Management (US026-US030).
 *
 * Endpoints:
 *   GET    /housing-units/{unitId}/meters          → full history
 *   GET    /housing-units/{unitId}/meters/active   → current active meter
 *   POST   /housing-units/{unitId}/meters          → assign first meter
 *   PUT    /housing-units/{unitId}/meters/replace  → replace meter
 *   DELETE /housing-units/{unitId}/meters/active   → remove meter
 */
@Injectable({ providedIn: 'root' })
export class WaterMeterService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * US028 AC2 — Full history sorted by installation date DESC.
   */
  getMeterHistory(unitId: number): Observable<WaterMeterHistory[]> {
    return this.http.get<WaterMeterHistory[]>(
      `${this.baseUrl}/housing-units/${unitId}/meters`
    );
  }

  /**
   * US026 AC1, US028 AC1 — Current active meter.
   * Returns null body with HTTP 204 when no meter assigned.
   */
  getActiveMeter(unitId: number): Observable<WaterMeterHistory | null> {
    return this.http.get<WaterMeterHistory | null>(
      `${this.baseUrl}/housing-units/${unitId}/meters/active`
    );
  }

  /**
   * US026 — Assign the first water meter to a unit.
   */
  assignMeter(unitId: number, request: AssignMeterRequest): Observable<WaterMeterHistory> {
    return this.http.post<WaterMeterHistory>(
      `${this.baseUrl}/housing-units/${unitId}/meters`,
      request
    );
  }

  /**
   * US027 — Replace the current active meter with a new one.
   */
  replaceMeter(unitId: number, request: ReplaceMeterRequest): Observable<WaterMeterHistory> {
    return this.http.put<WaterMeterHistory>(
      `${this.baseUrl}/housing-units/${unitId}/meters/replace`,
      request
    );
  }

  /**
   * US029 — Remove the active meter without replacement.
   */
  removeMeter(unitId: number, request: RemoveMeterRequest): Observable<WaterMeterHistory> {
    return this.http.delete<WaterMeterHistory>(
      `${this.baseUrl}/housing-units/${unitId}/meters/active`,
      { body: request }
    );
  }
}
