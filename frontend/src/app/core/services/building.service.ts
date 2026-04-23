import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from '../services/active-estate.service';
import {
  Building,
  CreateBuildingRequest,
  Page,
  UpdateBuildingRequest,
} from '../../models/building.model';

/**
 * Service for Building API operations.
 * UC004_ESTATE_PLACEHOLDER Phase 2: all endpoints scoped to the active estate.
 * Base URL: /api/v1/estates/{estateId}/buildings
 */
@Injectable({ providedIn: 'root' })
export class BuildingService {

  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id) throw new Error('No active estate — cannot call BuildingService without an estate context.');
    return id;
  }

  private get base(): string {
    return `/api/v1/estates/${this.estateId}/buildings`;
  }

  /**
   * GET /api/v1/estates/{estateId}/buildings
   * UC005.004 — View Buildings List / UC005.005 — Search Buildings.
   */
  getAllBuildings(
    page = 0,
    size = 20,
    sort = 'name,asc',
    city?: string,
    search?: string,
  ): Observable<Page<Building>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);
    if (city)   params = params.set('city', city);
    if (search) params = params.set('search', search);
    return this.http.get<Page<Building>>(this.base, { params });
  }

  /**
   * GET /api/v1/estates/{estateId}/buildings/{id}
   */
  getBuildingById(id: number): Observable<Building> {
    return this.http.get<Building>(`${this.base}/${id}`);
  }

  /**
   * GET /api/v1/estates/{estateId}/buildings/cities
   */
  getAllCities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.base}/cities`);
  }

  /**
   * POST /api/v1/estates/{estateId}/buildings
   * UC005.001 — Create Building.
   */
  createBuilding(request: CreateBuildingRequest): Observable<Building> {
    return this.http.post<Building>(this.base, request);
  }

  /**
   * PUT /api/v1/estates/{estateId}/buildings/{id}
   * UC005.002 — Edit Building.
   */
  updateBuilding(id: number, request: UpdateBuildingRequest): Observable<Building> {
    return this.http.put<Building>(`${this.base}/${id}`, request);
  }

  /**
   * DELETE /api/v1/estates/{estateId}/buildings/{id}
   * UC005.003 — Delete Building.
   */
  deleteBuilding(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.base}/${id}`);
  }
}
