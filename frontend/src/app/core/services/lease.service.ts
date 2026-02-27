// core/services/lease.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Lease, LeaseSummary, LeaseAlert,
  CreateLeaseRequest, UpdateLeaseRequest,
  AddTenantRequest, ChangeLeaseStatusRequest, AdjustRentRequest,
} from '../../models/lease.model';

@Injectable({ providedIn: 'root' })
export class LeaseService {
  private readonly base = '/api/v1';

  constructor(private http: HttpClient) {}

  getByUnit(unitId: number): Observable<LeaseSummary[]> {
    return this.http.get<LeaseSummary[]>(`${this.base}/housing-units/${unitId}/leases`);
  }

  getById(id: number): Observable<Lease> {
    return this.http.get<Lease>(`${this.base}/leases/${id}`);
  }

  create(req: CreateLeaseRequest, activate = false): Observable<Lease> {
    const params = new HttpParams().set('activate', activate);
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
    return this.http.delete<Lease>(`${this.base}/leases/${leaseId}/tenants/${personId}`);
  }

  adjustRent(leaseId: number, req: AdjustRentRequest): Observable<Lease> {
    return this.http.post<Lease>(`${this.base}/leases/${leaseId}/rent-adjustments`, req);
  }

  getAlerts(): Observable<LeaseAlert[]> {
    return this.http.get<LeaseAlert[]>(`${this.base}/leases/alerts`);
  }
}
