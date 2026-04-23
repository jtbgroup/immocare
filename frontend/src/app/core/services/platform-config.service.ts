// core/services/platform-config.service.ts — UC004_ESTATE_PLACEHOLDER Phase 5
// All endpoints migrated to /api/v1/estates/{estateId}/config/**
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BulkUpdateConfigRequest,
  PlatformConfigDTO,
  UpdateConfigRequest,
} from '../../models/platform-config.model';
import { ActiveEstateService } from './active-estate.service';

@Injectable({ providedIn: 'root' })
export class PlatformConfigService {

  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    const id = this.activeEstateService.activeEstateId();
    if (!id) throw new Error('No active estate — cannot call PlatformConfigService without an estate context.');
    return id;
  }

  private get base(): string {
    return `/api/v1/estates/${this.estateId}/config`;
  }

  // ─── Settings ─────────────────────────────────────────────────────────────

  /**
   * GET /api/v1/estates/{estateId}/config/settings
   * UC013.001 — List all config entries for the active estate.
   */
  getAll(): Observable<PlatformConfigDTO[]> {
    return this.http.get<PlatformConfigDTO[]>(`${this.base}/settings`);
  }

  /**
   * GET /api/v1/estates/{estateId}/config/settings/{key}
   */
  getOne(key: string): Observable<PlatformConfigDTO> {
    return this.http.get<PlatformConfigDTO>(`${this.base}/settings/${key}`);
  }

  /**
   * PUT /api/v1/estates/{estateId}/config/settings/{key}
   * UC013.004 — Update a single config entry.
   */
  updateOne(key: string, req: UpdateConfigRequest): Observable<PlatformConfigDTO> {
    return this.http.put<PlatformConfigDTO>(`${this.base}/settings/${key}`, req);
  }

  /**
   * Bulk update — kept for backward compatibility with SettingsComponent.
   * Delegates to individual PUT calls sequentially.
   * NOTE: If the backend exposes a bulk endpoint at this path, use it directly.
   */
  bulkUpdate(req: BulkUpdateConfigRequest): Observable<PlatformConfigDTO[]> {
    // The backend Phase 5 does not define a bulk endpoint — individual PUTs per entry.
    // If a bulk endpoint is added later, swap this implementation.
    throw new Error(
      'bulkUpdate is not supported in Phase 5. Use updateOne() per key or implement a backend bulk endpoint.',
    );
  }

  // ─── Boiler validity rules ────────────────────────────────────────────────

  /**
   * GET /api/v1/estates/{estateId}/config/boiler-validity-rules
   * UC013.003 — List validity rules for the active estate.
   */
  getValidityRules(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/boiler-validity-rules`);
  }

  /**
   * POST /api/v1/estates/{estateId}/config/boiler-validity-rules
   * UC013.002 — Add a boiler validity rule.
   */
  addValidityRule(req: any): Observable<any> {
    return this.http.post<any>(`${this.base}/boiler-validity-rules`, req);
  }

  // ─── Asset type mappings ─────────────────────────────────────────────────

  /**
   * GET /api/v1/estates/{estateId}/config/asset-type-mappings
   * UC012.001 — List asset type → subcategory mappings.
   */
  getAssetTypeMappings(): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/asset-type-mappings`);
  }

  /**
   * PUT /api/v1/estates/{estateId}/config/asset-type-mappings/{assetType}
   * UC012.001 — Update a mapping.
   */
  updateAssetTypeMapping(assetType: string, req: any): Observable<any> {
    return this.http.put<any>(`${this.base}/asset-type-mappings/${assetType}`, req);
  }
}
