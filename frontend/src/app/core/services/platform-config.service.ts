// core/services/platform-config.service.ts — UC012
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BulkUpdateConfigRequest,
  PlatformConfigDTO,
  UpdateConfigRequest,
} from '../../models/platform-config.model';

@Injectable({ providedIn: 'root' })
export class PlatformConfigService {

  constructor(private http: HttpClient) {}

  getAll(): Observable<PlatformConfigDTO[]> {
    return this.http.get<PlatformConfigDTO[]>('/api/v1/platform-config');
  }

  getOne(key: string): Observable<PlatformConfigDTO> {
    return this.http.get<PlatformConfigDTO>(`/api/v1/platform-config/${key}`);
  }

  updateOne(key: string, req: UpdateConfigRequest): Observable<PlatformConfigDTO> {
    return this.http.patch<PlatformConfigDTO>(`/api/v1/platform-config/${key}`, req);
  }

  bulkUpdate(req: BulkUpdateConfigRequest): Observable<PlatformConfigDTO[]> {
    return this.http.put<PlatformConfigDTO[]>('/api/v1/platform-config', req);
  }
}
