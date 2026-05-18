// core/services/alert.service.ts — UC004_ESTATE_PLACEHOLDER Phase 4
// Alerts are now estate-scoped.
import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { AlertDTO } from "../../models/alert.model";
import { ActiveEstateService } from "./active-estate.service";

@Injectable({ providedIn: "root" })
export class AlertService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get base(): string {
    const estateId = this.activeEstateService.activeEstateId();
    return `/api/v1/estates/${estateId}/alerts`;
  }

  /** Returns all pending alerts, sorted by deadline ASC. */
  getAlerts(): Observable<AlertDTO[]> {
    return this.http.get<AlertDTO[]>(this.base);
  }

  /** Returns the total alert count — used by the bell badge. */
  getCount(): Observable<number> {
    return this.http.get<number>(`${this.base}/count`);
  }
}
