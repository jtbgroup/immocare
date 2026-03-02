// core/services/alert.service.ts
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AlertDTO } from '../../models/alert.model';

@Injectable({ providedIn: 'root' })
export class AlertService {

  private readonly base = '/api/v1/alerts';

  constructor(private http: HttpClient) {}

  /** Returns all pending alerts, sorted by deadline ASC. */
  getAlerts(): Observable<AlertDTO[]> {
    return this.http.get<AlertDTO[]>(this.base);
  }

  /** Returns the total alert count — used by the bell badge. */
  getCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/count`);
  }
}
