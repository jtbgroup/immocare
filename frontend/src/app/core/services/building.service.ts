import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Building, CreateBuildingRequest, UpdateBuildingRequest, Page } from '../../models/building.model';
import { environment } from '../../../environments/environment';

/**
 * Service for Building API operations.
 * Handles all HTTP requests to the building endpoints.
 */
@Injectable({
  providedIn: 'root'
})
export class BuildingService {
  private readonly apiUrl = `${environment.apiUrl}/buildings`;

  constructor(private http: HttpClient) {}

  /**
   * Get all buildings with optional filters.
   * Implements US004 - View Buildings List and US005 - Search Buildings.
   *
   * @param page page number (0-indexed)
   * @param size page size
   * @param sort sort parameter (e.g., 'name,asc')
   * @param city optional city filter
   * @param search optional search term
   * @returns observable of paginated buildings
   */
  getAllBuildings(
    page: number = 0,
    size: number = 20,
    sort: string = 'name,asc',
    city?: string,
    search?: string
  ): Observable<Page<Building>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sort', sort);

    if (city) {
      params = params.set('city', city);
    }

    if (search) {
      params = params.set('search', search);
    }

    return this.http.get<Page<Building>>(this.apiUrl, { params });
  }

  /**
   * Get a building by ID.
   *
   * @param id the building ID
   * @returns observable of the building
   */
  getBuildingById(id: number): Observable<Building> {
    return this.http.get<Building>(`${this.apiUrl}/${id}`);
  }

  /**
   * Create a new building.
   * Implements US001 - Create Building.
   *
   * @param request the building creation request
   * @returns observable of the created building
   */
  createBuilding(request: CreateBuildingRequest): Observable<Building> {
    return this.http.post<Building>(this.apiUrl, request);
  }

  /**
   * Update an existing building.
   * Implements US002 - Edit Building.
   *
   * @param id the building ID
   * @param request the building update request
   * @returns observable of the updated building
   */
  updateBuilding(id: number, request: UpdateBuildingRequest): Observable<Building> {
    return this.http.put<Building>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete a building.
   * Implements US003 - Delete Building.
   *
   * @param id the building ID
   * @returns observable of the deletion response
   */
  deleteBuilding(id: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${id}`);
  }

  /**
   * Get all distinct cities for filtering.
   *
   * @returns observable of city names
   */
  getAllCities(): Observable<string[]> {
    return this.http.get<string[]>(`${this.apiUrl}/cities`);
  }
}
