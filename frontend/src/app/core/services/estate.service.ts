// core/services/estate.service.ts — UC004_ESTATE_PLACEHOLDER Phase 1
import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import {
  AddEstateMemberRequest,
  CreateEstateRequest,
  Estate,
  EstateDashboard,
  EstateMember,
  EstateSummary,
  UpdateEstateMemberRoleRequest,
  UpdateEstateRequest,
} from "../../models/estate.model";
import { Page } from "../../models/page.model";

@Injectable({ providedIn: "root" })
export class EstateService {
  private readonly adminBase = "/api/v1/admin/estates";
  private readonly base = "/api/v1/estates";

  constructor(private http: HttpClient) {}

  // ─── Admin endpoints (PLATFORM_ADMIN only) ───────────────────────────────

  getAllEstates(
    page = 0,
    size = 20,
    search?: string,
  ): Observable<Page<Estate>> {
    let params = new HttpParams().set("page", page).set("size", size);
    if (search?.trim()) params = params.set("search", search.trim());
    return this.http.get<Page<Estate>>(this.adminBase, { params });
  }

  getEstateById(id: string): Observable<Estate> {
    return this.http.get<Estate>(`${this.adminBase}/${id}`);
  }

  createEstate(req: CreateEstateRequest): Observable<Estate> {
    return this.http.post<Estate>(this.adminBase, req);
  }

  updateEstate(id: string, req: UpdateEstateRequest): Observable<Estate> {
    return this.http.put<Estate>(`/api/v1/estates/${id}`, req);
  }

  deleteEstate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.adminBase}/${id}`);
  }

  // ─── User endpoints ───────────────────────────────────────────────────────

  getMyEstates(): Observable<EstateSummary[]> {
    return this.http.get<EstateSummary[]>(`${this.base}/mine`);
  }

  getDashboard(estateId: string): Observable<EstateDashboard> {
    return this.http.get<EstateDashboard>(`${this.base}/${estateId}/dashboard`);
  }

  // ─── Member endpoints ─────────────────────────────────────────────────────

  getMembers(estateId: string): Observable<EstateMember[]> {
    return this.http.get<EstateMember[]>(`${this.base}/${estateId}/members`);
  }

  addMember(
    estateId: string,
    req: AddEstateMemberRequest,
  ): Observable<EstateMember> {
    return this.http.post<EstateMember>(
      `${this.base}/${estateId}/members`,
      req,
    );
  }

  updateMemberRole(
    estateId: string,
    userId: number,
    req: UpdateEstateMemberRoleRequest,
  ): Observable<EstateMember> {
    return this.http.patch<EstateMember>(
      `${this.base}/${estateId}/members/${userId}`,
      req,
    );
  }

  removeMember(estateId: string, userId: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${estateId}/members/${userId}`);
  }
}
