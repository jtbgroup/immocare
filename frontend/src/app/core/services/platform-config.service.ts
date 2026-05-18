import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  PlatformConfigDTO,
  UpdatePlatformConfigRequest,
} from '../../models/platform-config.model';

/**
 * Service for platform-wide configuration.
 *
 * Points to /api/v1/platform-config/** — accessible to PLATFORM_ADMIN only.
 * Estate-scoped config uses EstateConfigService instead.
 */
@Injectable({ providedIn: 'root' })
export class PlatformConfigService {

  private readonly base = '/api/v1/platform-config';

  constructor(private http: HttpClient) {}

  /**
   * GET /api/v1/platform-config/settings
   * Returns all platform config entries.
   */
  getAll(): Observable<PlatformConfigDTO[]> {
    return this.http.get<PlatformConfigDTO[]>(`${this.base}/settings`);
  }

  /**
   * GET /api/v1/platform-config/settings/{key}
   */
  getOne(key: string): Observable<PlatformConfigDTO> {
    return this.http.get<PlatformConfigDTO>(`${this.base}/settings/${key}`);
  }

  /**
   * PUT /api/v1/platform-config/settings/{key}
   * Updates a single platform config value.
   */
  updateOne(
    key: string,
    req: UpdatePlatformConfigRequest,
  ): Observable<PlatformConfigDTO> {
    return this.http.put<PlatformConfigDTO>(
      `${this.base}/settings/${key}`,
      req,
    );
  }
}
