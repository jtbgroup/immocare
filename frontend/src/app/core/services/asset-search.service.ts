import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

// ─── Search result DTOs ───────────────────────────────────────────────────────

export interface BoilerSearchResult {
  id: number;
  label: string;       // brand + model
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export interface FireExtinguisherSearchResult {
  id: number;
  label: string;       // identificationNumber
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export interface MeterSearchResult {
  id: number;
  label: string;       // meterNumber + type
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export type AssetSearchResult =
  | BoilerSearchResult
  | FireExtinguisherSearchResult
  | MeterSearchResult;

// ─── Service ──────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class AssetSearchService {
  private readonly api = '/api/v1';

  constructor(private http: HttpClient) {}

  searchBoilers(q: string, buildingId?: number | null): Observable<BoilerSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<BoilerSearchResult[]>(`${this.api}/boilers/search`, { params });
  }

  searchFireExtinguishers(q: string, buildingId?: number | null): Observable<FireExtinguisherSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<FireExtinguisherSearchResult[]>(`${this.api}/fire-extinguishers/search`, { params });
  }

  searchMeters(q: string, buildingId?: number | null): Observable<MeterSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<MeterSearchResult[]>(`${this.api}/meters/search`, { params });
  }
}
