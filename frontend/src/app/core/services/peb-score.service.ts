import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreatePebScoreRequest,
  PebImprovementDTO,
  PebScoreDTO,
} from '../../models/peb-score.model';

@Injectable({ providedIn: 'root' })
export class PebScoreService {
  private base = '/api/v1/housing-units';

  constructor(private http: HttpClient) {}

  addScore(unitId: number, request: CreatePebScoreRequest): Observable<PebScoreDTO> {
    return this.http.post<PebScoreDTO>(`${this.base}/${unitId}/peb-scores`, request);
  }

  getHistory(unitId: number): Observable<PebScoreDTO[]> {
    return this.http.get<PebScoreDTO[]>(`${this.base}/${unitId}/peb-scores`);
  }

  getCurrentScore(unitId: number): Observable<PebScoreDTO> {
    return this.http.get<PebScoreDTO>(`${this.base}/${unitId}/peb-scores/current`);
  }

  getImprovements(unitId: number): Observable<PebImprovementDTO> {
    return this.http.get<PebImprovementDTO>(`${this.base}/${unitId}/peb-scores/improvements`);
  }
}
