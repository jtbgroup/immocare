import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";

import {
  PlatformConfigDTO,
  UpdatePlatformConfigRequest,
} from "../../models/platform-config.model";

/**
 * Service for platform-wide configuration.
 *
 * Points to /api/v1/platform-config/** — accessible to PLATFORM_ADMIN only.
 * Estate-scoped config uses EstateConfigService instead.
 */
@Injectable({ providedIn: "root" })
export class PlatformConfigService {
  private readonly base = "/api/v1/platform-config";

  constructor(private http: HttpClient) {}

  /**
   * GET /api/v1/platform-config/settings
   * Returns all platform config entries.
   */
  getAll(): Observable<PlatformConfigDTO[]> {
    return this.http.get<PlatformConfigDTO[]>(`${this.base}/settings`);
  }

  /**
   * GET /api/v1/platform-config/settings/get?key=...
   */
  getOne(key: string): Observable<PlatformConfigDTO> {
    // 1. On prépare le paramètre 'key' pour la Query String
    const params = new HttpParams().set("key", key);

    // 2. On fait l'appel GET en passant les paramètres dans les options
    return this.http.get<PlatformConfigDTO>(`${this.base}/settings/get`, {
      params,
    });
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
