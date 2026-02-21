import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment";
import {
  CreateHousingUnitRequest,
  HousingUnit,
  UpdateHousingUnitRequest,
} from "../../models/housing-unit.model";

@Injectable({ providedIn: "root" })
export class HousingUnitService {
  private readonly baseUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  /**
   * Get all units in a building.
   * GET /api/v1/buildings/{buildingId}/units
   */
  getUnitsByBuilding(buildingId: number): Observable<HousingUnit[]> {
    return this.http.get<HousingUnit[]>(
      `${this.baseUrl}/buildings/${buildingId}/units`,
    );
  }

  /**
   * Get a single unit by ID.
   * GET /api/v1/units/{id}
   */
  getUnitById(id: number): Observable<HousingUnit> {
    return this.http.get<HousingUnit>(`${this.baseUrl}/units/${id}`);
  }

  /**
   * Create a new housing unit.
   * POST /api/v1/units
   */
  create(request: CreateHousingUnitRequest): Observable<HousingUnit> {
    return this.http.post<HousingUnit>(`${this.baseUrl}/units`, request);
  }

  /**
   * Update an existing housing unit.
   * PUT /api/v1/units/{id}
   */
  update(
    id: number,
    request: UpdateHousingUnitRequest,
  ): Observable<HousingUnit> {
    return this.http.put<HousingUnit>(`${this.baseUrl}/units/${id}`, request);
  }

  /**
   * Delete a housing unit.
   * DELETE /api/v1/units/{id}
   */
  delete(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/units/${id}`);
  }
}
