// core/services/asset-search.service.ts — UC004_ESTATE_PLACEHOLDER Phase 4
// Asset search endpoints are estate-scoped via buildings/units within the estate.
import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ActiveEstateService } from './active-estate.service';

export interface BoilerSearchResult {
  id: number;
  label: string;
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export interface FireExtinguisherSearchResult {
  id: number;
  label: string;
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export interface MeterSearchResult {
  id: number;
  label: string;
  unitNumber: string | null;
  buildingName: string;
  buildingId: number | null;
  housingUnitId: number | null;
}

export type AssetSearchResult =
  | BoilerSearchResult
  | FireExtinguisherSearchResult
  | MeterSearchResult;

@Injectable({ providedIn: 'root' })
export class AssetSearchService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estatePrefix(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return estateId ? `/api/v1/estates/${estateId}` : '/api/v1';
  }

  searchBoilers(q: string, buildingId?: number | null): Observable<BoilerSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<BoilerSearchResult[]>(`${this.estatePrefix}/boilers/search`, { params });
  }

  searchFireExtinguishers(q: string, buildingId?: number | null): Observable<FireExtinguisherSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<FireExtinguisherSearchResult[]>(`${this.estatePrefix}/fire-extinguishers/search`, { params });
  }

  searchMeters(q: string, buildingId?: number | null): Observable<MeterSearchResult[]> {
    let params = new HttpParams().set('q', q);
    if (buildingId != null) params = params.set('buildingId', buildingId);
    return this.http.get<MeterSearchResult[]>(`${this.estatePrefix}/meters/search`, { params });
  }
}
