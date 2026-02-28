// core/services/lease.service.ts
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

@Injectable({ providedIn: "root" })
export class LeaseService {
  private readonly base = "/api/v1";

  constructor(private http: HttpClient) {}

  getByUnit(unitId: number): Observable<LeaseSummary[]> {
    return this.http.get<LeaseSummary[]>(
      `${this.base}/housing-units/${unitId}/leases`,
    );
  }

  getById(id: number): Observable<Lease> {
    return this.http.get<Lease>(`${this.base}/leases/${id}`);
  }

  create(req: CreateLeaseRequest, activate = false): Observable<Lease> {
    const params = new HttpParams().set("activate", activate);
    return this.http.post<Lease>(`${this.base}/leases`, req, { params });
  }

  update(id: number, req: UpdateLeaseRequest): Observable<Lease> {
    return this.http.put<Lease>(`${this.base}/leases/${id}`, req);
  }

  changeStatus(id: number, req: ChangeLeaseStatusRequest): Observable<Lease> {
    return this.http.patch<Lease>(`${this.base}/leases/${id}/status`, req);
  }

  addTenant(leaseId: number, req: AddTenantRequest): Observable<Lease> {
    return this.http.post<Lease>(`${this.base}/leases/${leaseId}/tenants`, req);
  }

  removeTenant(leaseId: number, personId: number): Observable<Lease> {
    return this.http.delete<Lease>(
      `${this.base}/leases/${leaseId}/tenants/${personId}`,
    );
  }

  adjustRent(leaseId: number, req: AdjustRentRequest): Observable<Lease> {
    return this.http.post<Lease>(
      `${this.base}/leases/${leaseId}/rent-adjustments`,
      req,
    );
  }

  getAlerts(): Observable<LeaseAlert[]> {
    return this.http.get<LeaseAlert[]>(`${this.base}/leases/alerts`);
  }

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
      // Spring accepts repeated params: ?status=ACTIVE&status=DRAFT
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

    return this.http.get<Page<LeaseGlobalSummary>>(`${this.base}/leases`, {
      params,
    });
  }
}
