import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { environment } from "../../../environments/environment.prod";
import {
  CreateHousingUnitRequest,
  HousingUnit,
  UpdateHousingUnitRequest,
} from "../../models/housing-unit.model";
import { ActiveEstateService } from "../services/active-estate.service";

/**
 * Angular service for Housing Unit API calls.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all endpoints scoped to the active estate.
 *
 * Routes:
 *   GET    /api/v1/estates/{estateId}/buildings/{buildingId}/units
 *   GET    /api/v1/estates/{estateId}/units/{id}
 *   POST   /api/v1/estates/{estateId}/units
 *   PUT    /api/v1/estates/{estateId}/units/{id}
 *   DELETE /api/v1/estates/{estateId}/units/{id}
 */
@Injectable({ providedIn: "root" })
export class HousingUnitService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id)
      throw new Error(
        "No active estate — cannot call HousingUnitService without an estate context.",
      );
    return id;
  }

  private get baseUnits(): string {
    return environment.apiUrl + `/estates/${this.estateId}/units`;
  }

  private baseBuilding(buildingId: number): string {
    return (
      environment.apiUrl +
      `/estates/${this.estateId}/buildings/${buildingId}/units`
    );
  }

  /**
   * GET /api/v1/estates/{estateId}/buildings/{buildingId}/units
   */
  getUnitsByBuilding(buildingId: number): Observable<HousingUnit[]> {
    return this.http.get<HousingUnit[]>(this.baseBuilding(buildingId));
  }

  /**
   * GET /api/v1/estates/{estateId}/units/{id}
   */
  getUnitById(id: number): Observable<HousingUnit> {
    return this.http.get<HousingUnit>(`${this.baseUnits}/${id}`);
  }

  /**
   * POST /api/v1/estates/{estateId}/units
   */
  create(request: CreateHousingUnitRequest): Observable<HousingUnit> {
    return this.http.post<HousingUnit>(this.baseUnits, request);
  }

  /**
   * PUT /api/v1/estates/{estateId}/units/{id}
   */
  update(
    id: number,
    request: UpdateHousingUnitRequest,
  ): Observable<HousingUnit> {
    return this.http.put<HousingUnit>(`${this.baseUnits}/${id}`, request);
  }

  /**
   * DELETE /api/v1/estates/{estateId}/units/{id}
   */
  delete(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUnits}/${id}`);
  }
}
