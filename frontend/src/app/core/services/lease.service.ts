// core/services/lease.service.ts — UC016 Phase 3
import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  AddTenantRequest,
  AdjustRentRequest,
  ChangeLeaseStatusRequest,
  CreateLeaseRequest,
  Lease,
  LeaseAlert,
  LeaseGlobalFilters,
  LeaseGlobalSummary,
  LeaseSummary,
  UpdateLeaseRequest,
} from "../../models/lease.model";
import { Page } from "../../models/page.model";
import { ActiveEstateService } from "./active-estate.service";

@Injectable({ providedIn: "root" })
export class LeaseService {
  constructor(
    private http: HttpClient,
    private activeEstateService: ActiveEstateService,
  ) {}

  private get estateId(): string {
    return this.activeEstateService.activeEstateId()!;
  }

  private get base(): string {
    return `/api/v1/estates/${this.estateId}/leases`;
  }

  // ── Per-unit endpoint ────────────────────────────────────────────────────

  getByUnit(unitId: number): Observable<LeaseSummary[]> {
    return this.http.get<LeaseSummary[]>(
      `/api/v1/estates/${this.estateId}/housing-units/${unitId}/leases`,
    );
  }

  // ── CRUD ─────────────────────────────────────────────────────────────────

  getById(id: number): Observable<Lease> {
    return this.http.get<Lease>(`${this.base}/${id}`);
  }

  create(unitId: number, req: CreateLeaseRequest, activate = false): Observable<Lease> {
    const params = new HttpParams().set("activate", activate);
    return this.http.post<Lease>(
      `/api/v1/estates/${this.estateId}/housing-units/${unitId}/leases`,
      req,
      { params },
    );
  }

  update(id: number, req: UpdateLeaseRequest): Observable<Lease> {
    return this.http.put<Lease>(`${this.base}/${id}`, req);
  }

  changeStatus(id: number, req: ChangeLeaseStatusRequest): Observable<Lease> {
    return this.http.patch<Lease>(`${this.base}/${id}/status`, req);
  }

  // ── Tenants ───────────────────────────────────────────────────────────────

  addTenant(leaseId: number, req: AddTenantRequest): Observable<Lease> {
    return this.http.post<Lease>(`${this.base}/${leaseId}/tenants`, req);
  }

  removeTenant(leaseId: number, personId: number): Observable<Lease> {
    return this.http.delete<Lease>(
      `${this.base}/${leaseId}/tenants/${personId}`,
    );
  }

  // ── Rent adjustments ──────────────────────────────────────────────────────

  adjustRent(leaseId: number, req: AdjustRentRequest): Observable<Lease> {
    return this.http.post<Lease>(
      `${this.base}/${leaseId}/rent-adjustments`,
      req,
    );
  }

  // ── Alerts ────────────────────────────────────────────────────────────────

  /**
   * Returns contextual lease alerts for inline banners.
   * The global alerts page uses AlertService.getAlerts() instead.
   */
  getAlerts(): Observable<LeaseAlert[]> {
    return this.http.get<LeaseAlert[]>(`${this.base}/alerts`);
  }

  // ── Global list ───────────────────────────────────────────────────────────

  getAll(
    filters: LeaseGlobalFilters,
    page = 0,
    size = 20,
    sort = "startDate,desc",
  ): Observable<Page<LeaseGlobalSummary>> {
    let params = new HttpParams()
      .set("page", page)
      .set("size", size)
      .set("sort", sort);

    if (filters.statuses?.length) {
      filters.statuses.forEach((s) => {
        params = params.append("status", s);
      });
    }
    if (filters.leaseType) params = params.set("leaseType", filters.leaseType);
    if (filters.startDateFrom)
      params = params.set("startDateFrom", filters.startDateFrom);
    if (filters.startDateTo)
      params = params.set("startDateTo", filters.startDateTo);
    if (filters.endDateFrom)
      params = params.set("endDateFrom", filters.endDateFrom);
    if (filters.endDateTo) params = params.set("endDateTo", filters.endDateTo);
    if (filters.rentMin != null)
      params = params.set("rentMin", filters.rentMin);
    if (filters.rentMax != null)
      params = params.set("rentMax", filters.rentMax);

    return this.http.get<Page<LeaseGlobalSummary>>(this.base, { params });
  }
}
