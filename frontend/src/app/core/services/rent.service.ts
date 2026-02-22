import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { RentHistory, SetRentRequest } from '../../models/rent.model';

@Injectable({ providedIn: 'root' })
export class RentService {
  private baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * US023 — Full rent history for a unit, newest first.
   * GET /api/v1/housing-units/{unitId}/rents
   */
  getRentHistory(unitId: number): Observable<RentHistory[]> {
    return this.http.get<RentHistory[]>(
      `${this.baseUrl}/housing-units/${unitId}/rents`
    );
  }

  /**
   * US023 AC1 — Current active rent (effectiveTo = null).
   * GET /api/v1/housing-units/{unitId}/rents/current
   * Returns null body with 204 when no rent exists.
   */
  getCurrentRent(unitId: number): Observable<RentHistory | null> {
    return this.http.get<RentHistory | null>(
      `${this.baseUrl}/housing-units/${unitId}/rents/current`
    );
  }

  /**
   * US021 / US022 — Set initial rent or update existing rent.
   * POST /api/v1/housing-units/{unitId}/rents
   */
  setOrUpdateRent(unitId: number, request: SetRentRequest): Observable<RentHistory> {
    return this.http.post<RentHistory>(
      `${this.baseUrl}/housing-units/${unitId}/rents`,
      request
    );
  }
}
